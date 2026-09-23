package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobCheckedException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.module.autoexec.job.action.handler.AutoexecJobReFireHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 验证通用父作业动作按照实际父子关系递归执行。 */
public class AutoexecJobCascadeActionTest {

    private final List<Long> executedJobIdList = new ArrayList<>();
    private final List<String> executedJobActionList = new ArrayList<>();
    private IAutoexecJobActionHandler originalHandler;
    private IAutoexecJobActionHandler originalRefireHandler;
    private AutoexecJobServiceImpl service;

    /** 注入内存 Mapper 和动作处理器，避免连接数据库或 Runner。 */
    @Before
    public void setUp() throws Exception {
        service = new AutoexecJobServiceImpl();
        AutoexecJobMapper mapper = (AutoexecJobMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{AutoexecJobMapper.class},
                (proxy, method, args) -> {
                    Long jobId = args != null && args.length > 0 && args[0] instanceof Long ? (Long) args[0] : null;
                    if (method.getName().equals("getJobIdListByParentId")) {
                        return Long.valueOf(1L).equals(jobId) ? Collections.singletonList(2L) : Collections.emptyList();
                    }
                    if (method.getName().equals("getJobListLockByParentId")) {
                        if (Long.valueOf(1L).equals(jobId)) {
                            return Collections.singletonList(job(2L, JobStatus.CHECKED.getValue()));
                        }
                        if (Long.valueOf(2L).equals(jobId)) {
                            return Collections.singletonList(job(3L, JobStatus.FAILED.getValue()));
                        }
                        return Collections.emptyList();
                    }
                    return defaultValue(method.getReturnType());
                });
        set(service, "autoexecJobMapper", mapper);
        originalHandler = AutoexecJobActionHandlerFactory.getAction(JobAction.TAKE_OVER.getValue());
        originalRefireHandler = AutoexecJobActionHandlerFactory.getAction(JobAction.REFIRE.getValue());
        AutoexecJobActionHandlerFactory.getActionMap().put(JobAction.TAKE_OVER.getValue(), new RecordingHandler());
        AutoexecJobActionHandlerFactory.getActionMap().put(JobAction.REFIRE.getValue(), new RecordingRefireHandler());
    }

    /** 恢复全局动作处理器，避免影响其他测试。 */
    @After
    public void tearDown() {
        if (originalHandler == null) {
            AutoexecJobActionHandlerFactory.getActionMap().remove(JobAction.TAKE_OVER.getValue());
        } else {
            AutoexecJobActionHandlerFactory.getActionMap().put(JobAction.TAKE_OVER.getValue(), originalHandler);
        }
        if (originalRefireHandler == null) {
            AutoexecJobActionHandlerFactory.getActionMap().remove(JobAction.REFIRE.getValue());
        } else {
            AutoexecJobActionHandlerFactory.getActionMap().put(JobAction.REFIRE.getValue(), originalRefireHandler);
        }
    }

    /** 有子作业时执行父作业及全部层级的后代作业。 */
    @Test
    public void executesParentAndAllDescendants() throws Exception {
        AutoexecJobVo parentJob = job(1L);
        parentJob.setSource(neatlogic.framework.deploy.constvalue.JobSource.BATCHDEPLOY.getValue());
        parentJob.setAction(JobAction.TAKE_OVER.getValue());

        service.batchExecuteJobAction(parentJob, JobAction.TAKE_OVER);

        Assert.assertEquals(List.of(1L, 2L, 3L), executedJobIdList);
    }

    /** 没有子作业时只执行当前作业。 */
    @Test
    public void executesOnlyCurrentJobWithoutChildren() throws Exception {
        AutoexecJobVo job = job(4L);
        job.setSource(neatlogic.framework.deploy.constvalue.JobSource.BATCHDEPLOY.getValue());
        job.setAction(JobAction.TAKE_OVER.getValue());

        service.batchExecuteJobAction(job, JobAction.TAKE_OVER);

        Assert.assertEquals(Collections.singletonList(4L), executedJobIdList);
    }

    /** 未标记为批量的来源即使存在子作业也只执行当前作业。 */
    @Test
    public void doesNotCascadeForNonBatchSource() throws Exception {
        AutoexecJobVo parentJob = job(1L);
        parentJob.setSource(neatlogic.framework.autoexec.constvalue.JobSource.COMBOP.getValue());
        parentJob.setAction(JobAction.TAKE_OVER.getValue());

        service.batchExecuteJobAction(parentJob, JobAction.TAKE_OVER);

        Assert.assertEquals(Collections.singletonList(1L), executedJobIdList);
    }

    /** 普通作业仍按原有逻辑只重跑当前作业。 */
    @Test
    public void refiresOnlyCurrentJobForNonBatchSource() throws Exception {
        AutoexecJobVo job = job(1L);
        job.setSource(neatlogic.framework.autoexec.constvalue.JobSource.COMBOP.getValue());
        job.setAction(JobAction.REFIRE.getValue());

        service.batchExecuteJobAction(job, JobAction.REFIRE);

        Assert.assertEquals(Collections.singletonList(1L), executedJobIdList);
        Assert.assertEquals(Collections.singletonList(JobAction.REFIRE.getValue()), executedJobActionList);
    }

    /** 批量重跑由处理器跳过壳父作业和已验证后代，其余后代复用同一个重跑类型。 */
    @Test
    public void batchRefireSkipsShellParentAndExecutesDescendants() throws Exception {
        AutoexecJobVo parentJob = job(1L);
        parentJob.setSource(neatlogic.framework.deploy.constvalue.JobSource.BATCHDEPLOY.getValue());
        parentJob.setAction(JobAction.RESET_REFIRE.getValue());

        service.batchExecuteJobAction(parentJob, JobAction.REFIRE);

        Assert.assertEquals(Collections.singletonList(3L), executedJobIdList);
        Assert.assertEquals(Collections.singletonList(JobAction.RESET_REFIRE.getValue()), executedJobActionList);
    }

    /** 批量过滤钩子不改变单作业重跑已验证作业时的异常语义。 */
    @Test(expected = AutoexecJobCheckedException.class)
    public void directRefireOfCheckedJobStillFails() throws Exception {
        AutoexecJobVo job = job(5L, JobStatus.CHECKED.getValue());
        job.setSource(neatlogic.framework.autoexec.constvalue.JobSource.COMBOP.getValue());
        job.setAction(JobAction.REFIRE.getValue());

        new AutoexecJobReFireHandler().doService(job);
    }

    /** 发布来源的实例标识与原有静态批量判断保持一致。 */
    @Test
    public void deployBatchFlagMatchesExistingDefinition() {
        Assert.assertTrue(neatlogic.framework.deploy.constvalue.JobSource.BATCHDEPLOY.isBatch());
        Assert.assertTrue(neatlogic.framework.deploy.constvalue.JobSource.DEPLOY_SCHEDULE_PIPELINE.isBatch());
        Assert.assertTrue(neatlogic.framework.deploy.constvalue.JobSource.DEPLOY_CI_PIPELINE.isBatch());
        Assert.assertFalse(neatlogic.framework.deploy.constvalue.JobSource.DEPLOY.isBatch());
        Assert.assertFalse(neatlogic.framework.deploy.constvalue.JobSource.DEPLOY_SCHEDULE_GENERAL.isBatch());
        Assert.assertFalse(neatlogic.framework.deploy.constvalue.JobSource.DEPLOY_CI.isBatch());
    }

    /** 构造最小作业对象。 */
    private static AutoexecJobVo job(Long id) {
        AutoexecJobVo job = new AutoexecJobVo();
        job.setId(id);
        return job;
    }

    /** 构造带状态的最小作业对象。 */
    private static AutoexecJobVo job(Long id, String status) {
        AutoexecJobVo job = job(id);
        job.setStatus(status);
        return job;
    }

    /** 通过反射注入被测服务依赖。 */
    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** 为代理方法返回基础类型默认值。 */
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    /** 记录通用动作实际处理的作业顺序。 */
    private class RecordingHandler implements IAutoexecJobActionHandler {
        @Override
        public String getName() {
            return JobAction.TAKE_OVER.getValue();
        }

        @Override
        public boolean validate(AutoexecJobVo jobVo) {
            return true;
        }

        @Override
        public JSONObject doService(AutoexecJobVo jobVo) {
            executedJobIdList.add(jobVo.getId());
            executedJobActionList.add(jobVo.getAction());
            return null;
        }
    }

    /** 复用真实重跑处理器的批量过滤规则，仅替换实际执行以便记录。 */
    private class RecordingRefireHandler extends AutoexecJobReFireHandler {
        @Override
        public JSONObject doService(AutoexecJobVo jobVo) {
            executedJobIdList.add(jobVo.getId());
            executedJobActionList.add(jobVo.getAction());
            return null;
        }
    }
}
