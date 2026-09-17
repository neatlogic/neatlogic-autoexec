package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONObject;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Locale;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import neatlogic.framework.autoexec.exception.AutoexecJobOperationAuditNotFoundException;
import neatlogic.framework.util.SpringContextUtil;
import java.util.Arrays;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.module.autoexec.dao.mapper.AutoexecJobOperationAuditMapper;

/** 验证审计查询隔离、分页和 Mapper 绑定，不连接真实租户库。 */
public class AutoexecJobOperationAuditQueryTest {
    private Object originalContext;
    /** 初始化异常翻译所需的最小上下文，不启动真实应用。 */
    @Before
    public void setUp() throws Exception {
        Field field = SpringContextUtil.class.getDeclaredField("ctx"); field.setAccessible(true); originalContext = field.get(null);
        StaticMessageSource messages = new StaticMessageSource(); messages.setUseCodeAsDefaultMessage(true);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
        new SpringContextUtil().setApplicationContext(context);
    }
    /** 恢复共享上下文，避免影响其他测试。 */
    @After
    public void tearDown() throws Exception {
        Field field = SpringContextUtil.class.getDeclaredField("ctx"); field.setAccessible(true); field.set(null, originalContext);
    }

    /** 对其它作业的记录 ID 不返回目标明细。 */
    @Test
    public void foreignRecordCannotReadTargets() throws Exception {
        AutoexecJobOperationAuditQueryService service = service((proxy, method, args) -> {
            if (method.getName().equals("getAudit")) {
                Assert.assertEquals(1L, args[0]); Assert.assertEquals(99L, args[1]); return null;
            }
            Assert.fail("不存在的记录不应继续读取目标"); return null;
        });
        JSONObject request = request(); request.put("id", 99L);
        try { service.get(request); Assert.fail(); }
        catch (AutoexecJobOperationAuditNotFoundException expected) { /* 业务拒绝符合预期。 */ }
    }

    /** 组合条件清理用户前缀，失效页码回到末页。 */
    @Test
    public void searchNormalizesUserAndClampsPage() throws Exception {
        AutoexecJobOperationAuditQueryService service = service((proxy, method, args) -> {
            JSONObject query = (JSONObject) args[0];
            Assert.assertEquals("uuid", query.getString("operatorUuid")); Assert.assertEquals("upgrade.sql", query.getString("keyword"));
            if (method.getName().equals("countAudits")) { return 21; }
            Assert.assertEquals(20, query.getIntValue("startNum")); return Collections.emptyList();
        });
        JSONObject request = request(); request.put("operatorUuid", "user#uuid"); request.put("keyword", "upgrade.sql"); request.put("currentPage", 99);
        JSONObject result = service.search(request); Assert.assertEquals(2, result.getIntValue("currentPage"));
    }

    /** 归并动作查询业务编码，并保留对象类型条件。 */
    @Test
    public void actionFilterIncludesJobActionCodes() throws Exception {
        AutoexecJobOperationAuditQueryService service = service((proxy, method, args) -> {
            JSONObject query = (JSONObject) args[0];
            Assert.assertEquals("sql", query.getString("objectType"));
            Assert.assertEquals(Arrays.asList("resetNode", "resetSql"), query.get("actions"));
            if (method.getName().equals("countAudits")) { return 0; }
            return Collections.emptyList();
        });
        JSONObject request = request(); request.put("action", "reset"); request.put("objectType", "sql");
        JSONObject result = service.search(request);
        Assert.assertEquals(10, result.getJSONArray("actionList").size());
        request.put("action", "resetSql"); service.search(request);
    }

    /** 归并仅影响审计维度，保留业务编码并排除只读操作。 */
    @Test
    public void jobActionOwnsAuditMetadata() {
        Assert.assertEquals("fire", JobAction.FIRE.getValue());
        Assert.assertEquals("refire", JobAction.normalizeAuditAction("refirePhase"));
        Assert.assertFalse(JobAction.getAuditQueryValues("execute").contains("refirePhase"));
        Assert.assertEquals("refire", JobAction.normalizeAuditAction("refireResetAll"));
        Assert.assertEquals("ignore", JobAction.normalizeAuditAction("ignorePhase"));
        Assert.assertEquals("interact", JobAction.normalizeAuditAction("submitNodeWaitInput"));
        Assert.assertEquals(Arrays.asList("refireResetAll", "refireNode", "refirePhase", "refireAll"), JobAction.getAuditQueryValues("refire"));
        Assert.assertFalse(JobAction.DOWNLOAD_NODE_LOG.isAuditAction());
        Assert.assertFalse(JobAction.INFORM_PHASE_ROUND.isAuditAction());
        Assert.assertEquals(Arrays.asList("unknown"), JobAction.getAuditQueryValues("unknown"));
    }

    /** 大整数标识始终作为字符串输出，时间仍为数值。 */
    @Test
    public void largeIdentifiersKeepPrecision() {
        AutoexecJobOperationAuditVo audit = new AutoexecJobOperationAuditVo(); audit.setId(Long.MAX_VALUE); audit.setJobId(Long.MAX_VALUE); audit.setOperateTime(1000L);
        JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(audit));
        Assert.assertEquals(Long.toString(Long.MAX_VALUE), result.get("id")); Assert.assertTrue(result.get("operateTime") instanceof Number);
    }

    /** Mapper 可解析且搜索文本始终作为绑定参数处理。 */
    @Test
    public void mapperBindsKeywordAndJobBoundary() throws Exception {
        String resource = "neatlogic/module/autoexec/dao/mapper/AutoexecJobOperationAuditMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        JSONObject request = request(); request.put("keyword", "' OR 1=1 --"); request.put("startNum", 0); request.put("pageSize", 20);
        String sql = configuration.getMappedStatement(AutoexecJobOperationAuditMapper.class.getName() + ".searchAudits").getBoundSql(request).getSql();
        Assert.assertTrue(sql.contains("a.job_id=?")); Assert.assertFalse(sql.contains("OR 1=1"));
        String deleteTargets = configuration.getMappedStatement(AutoexecJobOperationAuditMapper.class.getName() + ".deleteTargetsByJobId").getBoundSql(request).getSql();
        Assert.assertTrue(deleteTargets.contains("a.id=t.audit_id"));
        Assert.assertTrue(deleteTargets.contains("a.job_id=?"));
        String deleteAudits = configuration.getMappedStatement(AutoexecJobOperationAuditMapper.class.getName() + ".deleteAuditsByJobId").getBoundSql(request).getSql();
        Assert.assertTrue(deleteAudits.contains("WHERE job_id=?"));
    }

    /** 用户名称实时查询，同页复用；改名后更新，用户删除后回退 UUID。 */
    @Test
    public void operatorNameUsesCurrentUserAndFallsBackToUuid() throws Exception {
        AutoexecJobOperationAuditVo audit = new AutoexecJobOperationAuditVo();
        audit.setOperatorUuid("operator-1"); audit.setAction("fire"); audit.setObjectType("job");
        AutoexecJobOperationAuditQueryService service = service((proxy, method, args) ->
                method.getName().equals("countAudits") ? 2 : Arrays.asList(audit, audit));
        UserVo user = new UserVo(); user.setUserName("当前用户");
        UserVo[] current = {user}; int[] calls = {0};
        UserMapper users = (UserMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UserMapper.class},
                (proxy, method, args) -> { calls[0]++; Assert.assertEquals("operator-1", args[0]); return current[0]; });
        set(service, "userMapper", users);
        Assert.assertEquals("当前用户", service.search(request()).getJSONArray("tbodyList").getJSONObject(0).getString("operatorName"));
        Assert.assertEquals(1, calls[0]);
        user.setUserName("更新名称");
        Assert.assertEquals("更新名称", service.search(request()).getJSONArray("tbodyList").getJSONObject(0).getString("operatorName"));
        current[0] = null;
        JSONObject row = service.search(request()).getJSONArray("tbodyList").getJSONObject(0);
        Assert.assertEquals("operator-1", row.getString("operatorName"));
        for (String field : Arrays.asList("jobName", "scope", "result", "resultText", "failureCode", "failureText")) {
            Assert.assertFalse(row.containsKey(field));
        }
    }

    /** 注入仅用于测试的只读 Mapper。 */
    private AutoexecJobOperationAuditQueryService service(java.lang.reflect.InvocationHandler handler) throws Exception {
        AutoexecJobOperationAuditQueryService service = new AutoexecJobOperationAuditQueryService();
        AutoexecJobMapper jobs = (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecJobMapper.class}, (proxy, method, args) -> new AutoexecJobVo());
        AutoexecJobOperationAuditMapper audits = (AutoexecJobOperationAuditMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecJobOperationAuditMapper.class}, handler);
        set(service, "jobMapper", jobs); set(service, "mapper", audits); return service;
    }
    /** 构造模拟作业查询。 */
    private JSONObject request() { JSONObject request = new JSONObject(); request.put("jobId", 1L); return request; }
    /** 注入隔离替身。 */
    private void set(Object object, String name, Object value) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }
}
