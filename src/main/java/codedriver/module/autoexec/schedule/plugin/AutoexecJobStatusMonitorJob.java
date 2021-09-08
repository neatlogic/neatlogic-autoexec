/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobProcessTaskStepVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import codedriver.framework.process.stephandler.core.IProcessStepHandler;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerFactory;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.autoexec.constvalue.AutoexecProcessStepHandlerType;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/9/6 19:35
 **/
@Component
public class AutoexecJobStatusMonitorJob extends JobBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-AUTOEXEC-JOB-STATUS-MONITOR";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
        Long autoexecJobId = (Long) jobObject.getData("autoexecJobId");
        AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo = autoexecJobMapper.getAutoexecJobProcessTaskStepByAutoexecJobId(autoexecJobId);
        if (autoexecJobProcessTaskStepVo == null) {
            return false;
        }
        if (Objects.equals(autoexecJobProcessTaskStepVo.getNeedMonitorStatus(), 0)) {
            return true;
        }
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        Long autoexecJobId = (Long) jobObject.getData("autoexecJobId");
        AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo = autoexecJobMapper.getAutoexecJobProcessTaskStepByAutoexecJobId(autoexecJobId);
        if (autoexecJobProcessTaskStepVo != null && Objects.equals(autoexecJobProcessTaskStepVo.getNeedMonitorStatus(), 1)) {
            AutoexecJobVo autoexecJobVo = autoexecJobMapper.getJobInfo(autoexecJobId);
            if (autoexecJobVo != null) {
                if (JobStatus.PENDING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.RUNNING.getValue().equals(autoexecJobVo.getStatus())) {
                    JobObject.Builder newJobObjectBuilder = new JobObject.Builder(autoexecJobId.toString(), this.getGroupName(), this.getClassName(), tenantUuid)
                            .withBeginTime(new Date())
                            .withIntervalInSeconds(60 * 60)
                            .withRepeatCount(-1)
                            .addData("autoexecJobId", autoexecJobId);
                    JobObject newJobObject = newJobObjectBuilder.build();
                    schedulerManager.loadJob(newJobObject);
                } else if (JobStatus.PAUSING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.PAUSED.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.ABORTING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.ABORTED.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.COMPLETED.getValue().equals(autoexecJobVo.getStatus())) {
                    processTaskStepcomplete(autoexecJobProcessTaskStepVo);
                } else if (JobStatus.FAILED.getValue().equals(autoexecJobVo.getStatus())) {

                }
            } else {
                autoexecJobMapper.updateAutoexecJobProcessTaskStepNoNeedMonitorStatusByAutoexecJobId(autoexecJobId);
            }
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        List<Long> autoexecJobIdList = autoexecJobMapper.getAllAutoexecJobStatusMonitorAutoexecJobId();
        for (Long autoexecJobId : autoexecJobIdList) {
            JobObject.Builder jobObjectBuilder = new JobObject
                    .Builder(autoexecJobId.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid())
                    .addData("autoexecJobId", autoexecJobId);
            JobObject jobObject = jobObjectBuilder.build();
            this.reloadJob(jobObject);
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        Long autoexecJobId = (Long) jobObject.getData("autoexecJobId");
        AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo = autoexecJobMapper.getAutoexecJobProcessTaskStepByAutoexecJobId(autoexecJobId);
        if (autoexecJobProcessTaskStepVo != null && Objects.equals(autoexecJobProcessTaskStepVo.getNeedMonitorStatus(), 1)) {
            AutoexecJobVo autoexecJobVo = autoexecJobMapper.getJobInfo(autoexecJobId);
            if (autoexecJobVo != null) {
                if (JobStatus.PENDING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.RUNNING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.PAUSING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.PAUSED.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.ABORTING.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.ABORTED.getValue().equals(autoexecJobVo.getStatus())) {

                } else if (JobStatus.COMPLETED.getValue().equals(autoexecJobVo.getStatus())) {
                    processTaskStepcomplete(autoexecJobProcessTaskStepVo);
                } else if (JobStatus.FAILED.getValue().equals(autoexecJobVo.getStatus())) {

                }
            }
        }
    }

    private void processTaskStepcomplete(AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo) {
        IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(AutoexecProcessStepHandlerType.AUTOEXEC.getHandler());
        if (handler != null) {
            try {
                ProcessTaskStepVo processTaskStepVo = new ProcessTaskStepVo();
                processTaskStepVo.setProcessTaskId(autoexecJobProcessTaskStepVo.getProcessTaskId());
                processTaskStepVo.setId(autoexecJobProcessTaskStepVo.getProcessTaskStepId());
                handler.complete(processTaskStepVo);
            } catch (ProcessTaskNoPermissionException e) {
//                throw new PermissionDeniedException();
            }
        }
    }
}
