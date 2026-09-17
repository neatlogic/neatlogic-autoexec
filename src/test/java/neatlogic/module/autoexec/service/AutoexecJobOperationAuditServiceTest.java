package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;
import neatlogic.framework.autoexec.job.audit.JobOperationAuditContext;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.exception.AutoexecJobCanNotFireException;
import neatlogic.framework.autoexec.exception.AutoexecJobCanNotRevokeException;
import neatlogic.module.autoexec.job.action.handler.AutoexecJobRevokeHandler;
import neatlogic.module.autoexec.job.action.handler.AutoexecJobSqlResetHandler;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.util.SpringContextUtil;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.context.support.MessageSourceAccessor;

/** 使用内存替身验证审计事务边界，不连接数据库或执行 Runner。 */
public class AutoexecJobOperationAuditServiceTest {
    private AutoexecJobOperationAuditService service;
    private MemoryStore store;
    private boolean storeUnavailable;
    private boolean supplementUnavailable;
    private Object originalContext;

    /** 创建固定作业与目标快照，隔离所有外部系统。 */
    @Before
    public void setUp() throws Exception {
        Field contextField = SpringContextUtil.class.getDeclaredField("ctx");
        contextField.setAccessible(true); originalContext = contextField.get(null);
        StaticMessageSource messages = new StaticMessageSource(); messages.setUseCodeAsDefaultMessage(true);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
        new SpringContextUtil().setApplicationContext(context);
        service = new AutoexecJobOperationAuditService();
        store = new MemoryStore();
        AutoexecJobVo job = new AutoexecJobVo(); job.setId(1L); job.setName("audit-test"); job.setStatus("completed");
        AutoexecJobMapper mapper = (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecJobMapper.class},
                (proxy, method, args) -> method.getName().equals("getJobInfo") ? job : null);
        set(service, "jobMapper", mapper); set(service, "store", store);
        set(service, "snapshots", new AutoexecJobOperationAuditSnapshotService() {
            @Override
            public AutoexecJobPhaseVo phase(JSONObject request, Long jobId) { return null; }
            @Override
            public List<AutoexecJobOperationAuditTargetVo> targets(AutoexecJobVo job, AutoexecJobPhaseVo phase, String objectType, JSONObject request) {
                List<AutoexecJobOperationAuditTargetVo> result = new ArrayList<>();
                for (long id : new long[]{10L, 11L}) {
                    AutoexecJobOperationAuditTargetVo target = new AutoexecJobOperationAuditTargetVo();
                    target.setObjectId(id); target.setNodeName("node-" + id); result.add(target);
                }
                return result;
            }
        });
        UserContext user = UserContext.init((UserContext) null); user.setUserUuid("operator-1"); user.setUserName("Operator"); UserContext.init(user);
    }

    /** 清理事务和请求线程数据，防止测试之间相互影响。 */
    @After
    public void tearDown() throws Exception {
        JobOperationAuditContext.close();
        if (TransactionSynchronizationManager.isSynchronizationActive()) { TransactionSynchronizationManager.clearSynchronization(); }
        UserContext.init((UserContext) null);
        Field contextField = SpringContextUtil.class.getDeclaredField("ctx");
        contextField.setAccessible(true); contextField.set(null, originalContext);
    }

    /** 内部调用只创建一条记录，普通操作无需补充写入。 */
    @Test
    public void nestedOperationCreatesOneRecord() throws Exception {
        Assert.assertEquals("original", service.execute(JobAction.FIRE, request(),
                () -> service.execute(JobAction.REFIRE, request(), () -> "original")));
        Assert.assertEquals(1, store.starts); Assert.assertEquals(0, store.finishes);
        Assert.assertEquals(2, store.last.getTargetCount().intValue());
        Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 无需 API 标识即可采集白名单动作，连续顶层操作各记一次。 */
    @Test
    public void handlerCapturesDirectCalls() throws Exception {
        AutoexecJobActionHandlerBase handler = handler(JobAction.REFIRE_NODE, true);
        AutoexecJobVo job = new AutoexecJobVo(); job.setId(1L);
        handler.doService(job); handler.doService(job);
        Assert.assertEquals(2, store.starts);
        Assert.assertEquals(JobAction.REFIRE_NODE.getValue(), store.last.getAction());
        Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 验证失败仍保留请求快照，并可靠清理上下文。 */
    @Test
    public void validationFailureClearsContext() throws Exception {
        AutoexecJobVo job = new AutoexecJobVo(); job.setId(1L);
        try {
            handler(JobAction.FIRE, false).doService(job);
            Assert.fail();
        } catch (AutoexecJobCanNotFireException expected) {
            Assert.assertEquals(1, store.starts);
        }
        Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 只读处理器不采集。 */
    @Test
    public void readOnlyHandlerDoesNotCapture() throws Exception {
        handler(JobAction.DOWNLOAD_NODE_LOG, true).doService(new AutoexecJobVo());
        Assert.assertEquals(0, store.starts);
        Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 撤销处理器通过公共链采集，原有作业状态限制保持有效。 */
    @Test
    public void revokeUsesCommonChainAndPreservesValidation() throws Exception {
        AutoexecJobRevokeHandler handler = new AutoexecJobRevokeHandler();
        Field field = AutoexecJobActionHandlerBase.class.getDeclaredField("operationAuditService");
        field.setAccessible(true); field.set(handler, service);
        AutoexecJobVo job = new AutoexecJobVo(); job.setId(1L); job.setStatus("completed");
        job.setActionParam(request());
        try { handler.doService(job); Assert.fail(); }
        catch (AutoexecJobCanNotRevokeException expected) {
            Assert.assertEquals(JobAction.REVOKE.getValue(), store.last.getAction());
        }
        Assert.assertEquals(1, store.starts);
        Assert.assertNull(JobOperationAuditContext.current());
    }

    /** SQL 重置按当前作业定位阶段，缺失阶段时不调用来源处理器。 */
    @Test
    public void sqlResetUsesCommonChainAndRejectsMissingPhase() throws Exception {
        AutoexecJobSqlResetHandler handler = new AutoexecJobSqlResetHandler();
        Field auditField = AutoexecJobActionHandlerBase.class.getDeclaredField("operationAuditService");
        auditField.setAccessible(true); auditField.set(handler, service);
        Field mapperField = AutoexecJobActionHandlerBase.class.getDeclaredField("autoexecJobMapper");
        mapperField.setAccessible(true); Object previous = mapperField.get(null);
        AutoexecJobMapper mapper = (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecJobMapper.class},
                (proxy, method, args) -> { Assert.assertEquals(1L, args[0]); return java.util.Collections.emptyList(); });
        mapperField.set(null, mapper);
        AutoexecJobVo job = new AutoexecJobVo(); job.setId(1L); job.setActionParam(request());
        try {
            handler.doService(job); Assert.fail();
        } catch (AutoexecJobPhaseNotFoundException expected) {
            Assert.assertEquals(JobAction.RESET_SQL.getValue(), store.last.getAction());
        } finally {
            mapperField.set(null, previous);
        }
        Assert.assertEquals(1, store.starts);
        Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 创建隔离业务处理器，测试真实公共处理链而不访问 Runner。 */
    private AutoexecJobActionHandlerBase handler(JobAction action, boolean valid) throws Exception {
        AutoexecJobActionHandlerBase handler = new AutoexecJobActionHandlerBase() {
            @Override
            public String getName() { return action.getValue(); }
            @Override
            public boolean validate(AutoexecJobVo job) { return valid; }
            @Override
            public JSONObject doMyService(AutoexecJobVo job) { return new JSONObject(); }
        };
        Field field = AutoexecJobActionHandlerBase.class.getDeclaredField("operationAuditService");
        field.setAccessible(true); field.set(handler, service);
        return handler;
    }

    /** 接管只补充上下文，不再注册提交或回滚结果回调。 */
    @Test
    public void takeoverSupplementsContextWithoutTransactionTracking() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        service.execute(JobAction.TAKE_OVER, request(), () -> null);
        Assert.assertEquals(1, store.finishes);
        Assert.assertEquals("operator-1", store.last.getCurrentExecUser());
        Assert.assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    /** 补充写入失败不改变业务返回，也不触发重复执行。 */
    @Test
    public void supplementFailurePreservesBusinessReturn() throws Exception {
        supplementUnavailable = true;
        Assert.assertEquals("done", service.execute(JobAction.TAKE_OVER, request(), () -> "done"));
        Assert.assertEquals(1, store.starts); Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 失败异常对象原样透传，记录不保存潜在敏感的异常正文。 */
    @Test
    public void failurePreservesExceptionAndRedactsMessage() throws Exception {
        Exception original = new Exception("password=secret SQL正文");
        try { service.execute(JobAction.FIRE, request(), () -> { throw original; }); Assert.fail(); }
        catch (Exception ex) { Assert.assertSame(original, ex); }
        Assert.assertEquals(1, store.starts); Assert.assertEquals(0, store.finishes);
        Assert.assertNull(store.last.getCurrentExecUser());
        Assert.assertFalse(JSON.toJSONString(store.last).contains("secret")); Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 审计写入失败不阻止业务执行，也不重试业务。 */
    @Test
    public void unavailableStoreDoesNotChangeBusinessReturn() throws Exception {
        storeUnavailable = true;
        Assert.assertEquals("done", service.execute(JobAction.FIRE, request(), () -> "done"));
        Assert.assertEquals(0, store.finishes); Assert.assertNull(JobOperationAuditContext.current());
    }

    /** 自由输入脱敏，只有服务端定义的动态按钮可以保留。 */
    @Test
    public void interactionStoresOnlyWhitelistedButton() throws Exception {
        JSONObject definition = new JSONObject(); definition.put("opType", "input");
        service.execute(JobAction.SUBMIT_NODE_WAIT_INPUT, request(), () -> { JobOperationAuditContext.interaction(definition, "secret"); return null; });
        Assert.assertEquals("[REDACTED]", store.last.getInteractionValue());
        definition.put("opType", "button"); definition.put("options", Arrays.asList("continue", "abort"));
        service.execute(JobAction.SUBMIT_NODE_WAIT_INPUT, request(), () -> { JobOperationAuditContext.interaction(definition, "continue"); return null; });
        Assert.assertEquals("continue", store.last.getInteractionValue());
    }

    /** 选择项必须来自服务端定义，多选中混入任意文本时整体脱敏。 */
    @Test
    public void selectionsAreWhitelistedAndUnknownValuesAreRedacted() throws Exception {
        JSONObject definition = new JSONObject(); definition.put("opType", "mselect"); definition.put("options", Arrays.asList("a", "b"));
        service.execute(JobAction.SUBMIT_NODE_WAIT_INPUT, request(), () -> { JobOperationAuditContext.interaction(definition, "[\"a\",\"b\"]"); return null; });
        Assert.assertEquals("[\"a\",\"b\"]", store.last.getInteractionValue());
        service.execute(JobAction.SUBMIT_NODE_WAIT_INPUT, request(), () -> { JobOperationAuditContext.interaction(definition, "[\"a\",\"secret\"]"); return null; });
        Assert.assertEquals("[REDACTED]", store.last.getInteractionValue());
    }

    /** 创建不包含真实业务参数的请求。 */
    private JSONObject request() { JSONObject request = new JSONObject(); request.put("jobId", 1L); return request; }
    /** 注入测试替身，不引入新的生产依赖。 */
    private void set(Object object, String name, Object value) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }

    /** 内存存储只保留快照，不实际开启数据库事务。 */
    private class MemoryStore extends AutoexecJobOperationAuditStore {
        int starts; int finishes; AutoexecJobOperationAuditVo last; List<AutoexecJobOperationAuditTargetVo> targets;
        /** 模拟初始记录保存及存储故障。 */
        @Override
        public void begin(AutoexecJobOperationAuditVo audit, List<AutoexecJobOperationAuditTargetVo> values) {
            if (storeUnavailable) { throw new IllegalStateException("模拟审计存储不可用"); }
            starts++; last = JSON.parseObject(JSON.toJSONString(audit), AutoexecJobOperationAuditVo.class);
            targets = JSON.parseArray(JSON.toJSONString(values), AutoexecJobOperationAuditTargetVo.class);
        }
        /** 复制最终快照，避免测试依赖可变引用。 */
        @Override
        public void supplement(AutoexecJobOperationAuditVo audit) {
            if (supplementUnavailable) { throw new IllegalStateException("模拟上下文存储不可用"); }
            finishes++; last = JSON.parseObject(JSON.toJSONString(audit), AutoexecJobOperationAuditVo.class);
        }
    }
}
