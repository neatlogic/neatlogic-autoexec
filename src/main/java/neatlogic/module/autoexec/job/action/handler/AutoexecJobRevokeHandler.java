package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import org.springframework.stereotype.Service;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.exception.AutoexecJobCanNotRevokeException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotSupportedExecuteAndRevokeException;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import javax.annotation.Resource;
import java.util.Arrays;

/** 撤销待执行作业并卸载定时任务，沿用原入口校验。 */
@Service
public class AutoexecJobRevokeHandler extends AutoexecJobActionHandlerBase {
    @Resource private SchedulerManager schedulerManager;

    @Override
    public String getName() { return JobAction.REVOKE.getValue(); }

    /** 执行原有业务逻辑，采集由公共 doService 统一负责。 */
    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        if (!JobStatus.READY.getValue().equals(jobVo.getStatus()) || !UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
            throw new AutoexecJobCanNotRevokeException(jobVo.getId());
        }
        if (!Arrays.asList(neatlogic.framework.deploy.constvalue.CombopOperationType.PIPELINE.getValue(), CombopOperationType.COMBOP.getValue()).contains(jobVo.getOperationType())) {
            throw new AutoexecJobNotSupportedExecuteAndRevokeException();
        }
        jobVo.setStatus(JobStatus.REVOKED.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
        IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
        }
        JobObject.Builder jobObjectBuilder = new JobObject.Builder(jobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
        schedulerManager.unloadJob(jobObjectBuilder.build());
        return null;
    }
}
