/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobProcessTaskStepVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import codedriver.framework.process.service.ProcessTaskService;
import codedriver.framework.process.stephandler.core.IProcessStepHandler;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerFactory;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.autoexec.constvalue.AutoexecProcessStepHandlerType;
import codedriver.module.autoexec.constvalue.FailPolicy;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
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
    @Resource
    private ProcessTaskService processTaskService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-AUTOEXEC-JOB-STATUS-MONITOR";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
//        System.out.println("isHealthy");
//        System.out.println(TenantContext.get().getTenantUuid());
//        Long autoexecJobId = (Long) jobObject.getData("autoexecJobId");
//        System.out.println(autoexecJobId);
//        AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo = autoexecJobMapper.getAutoexecJobProcessTaskStepByAutoexecJobId(autoexecJobId);
//        if (autoexecJobProcessTaskStepVo == null) {
//            return false;
//        }
//        if (Objects.equals(autoexecJobProcessTaskStepVo.getNeedMonitorStatus(), 0)) {
//            return true;
//        }
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        Long autoexecJobId = (Long) jobObject.getData("autoexecJobId");
        AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo = autoexecJobMapper.getAutoexecJobProcessTaskStepByAutoexecJobId(autoexecJobId);
        if (autoexecJobProcessTaskStepVo != null && Objects.equals(autoexecJobProcessTaskStepVo.getNeedMonitorStatus(), 1)) {
            AutoexecJobVo autoexecJobVo = autoexecJobMapper.getJobInfo(autoexecJobId);
            if (autoexecJobVo != null) {
                if (JobStatus.PENDING.getValue().equals(autoexecJobVo.getStatus()) || JobStatus.RUNNING.getValue().equals(autoexecJobVo.getStatus())) {
                    JobObject.Builder newJobObjectBuilder = new JobObject.Builder(autoexecJobId.toString(), this.getGroupName(), this.getClassName(), tenantUuid)
                            .withBeginTime(new Date())
                            .withIntervalInSeconds(60)
                            .addData("autoexecJobId", autoexecJobId);
                    JobObject newJobObject = newJobObjectBuilder.build();
                    schedulerManager.loadJob(newJobObject);
                } else if (JobStatus.COMPLETED.getValue().equals(autoexecJobVo.getStatus())) {
                    processTaskStepComplete(autoexecJobProcessTaskStepVo);
                    autoexecJobMapper.updateAutoexecJobProcessTaskStepNeedMonitorStatusByAutoexecJobId(autoexecJobId, 0);
                } else {
                    //暂停中、已暂停、中止中、已中止、已完成、已失败都属于异常，根据失败策略处理
                    if (FailPolicy.KEEP_ON.getValue().equals(autoexecJobProcessTaskStepVo.getFailPolicy())) {
                        int flag = processTaskStepComplete(autoexecJobProcessTaskStepVo);
                        if (flag == 1) {
                            autoexecJobMapper.updateAutoexecJobProcessTaskStepNeedMonitorStatusByAutoexecJobId(autoexecJobId, 0);
                        }
                    }
                }
            } else {
                autoexecJobMapper.updateAutoexecJobProcessTaskStepNeedMonitorStatusByAutoexecJobId(autoexecJobId, 0);
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
                if (JobStatus.PENDING.getValue().equals(autoexecJobVo.getStatus()) || JobStatus.RUNNING.getValue().equals(autoexecJobVo.getStatus())) {
                    //继续监听作业状态
                } else if (JobStatus.COMPLETED.getValue().equals(autoexecJobVo.getStatus())) {
                    processTaskStepComplete(autoexecJobProcessTaskStepVo);
                    autoexecJobMapper.updateAutoexecJobProcessTaskStepNeedMonitorStatusByAutoexecJobId(autoexecJobId, 0);
                    schedulerManager.unloadJob(jobObject);
                } else {
                    //暂停中、已暂停、中止中、已中止、已完成、已失败都属于异常，根据失败策略处理
                    if (FailPolicy.KEEP_ON.getValue().equals(autoexecJobProcessTaskStepVo.getFailPolicy())) {
                        int flag = processTaskStepComplete(autoexecJobProcessTaskStepVo);
                        if (flag == 1) {
                            autoexecJobMapper.updateAutoexecJobProcessTaskStepNeedMonitorStatusByAutoexecJobId(autoexecJobId, 0);
                            schedulerManager.unloadJob(jobObject);
                        }
                    }
                }
            }
        }
    }

    private int processTaskStepComplete(AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo) {
        List<ProcessTaskStepVo> processTaskStepList = processTaskService.getForwardNextStepListByProcessTaskStepId(autoexecJobProcessTaskStepVo.getProcessTaskStepId());
        if (processTaskStepList.size() == 1) {
            ProcessTaskStepVo nextStepVo = processTaskStepList.get(0);
            IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(AutoexecProcessStepHandlerType.AUTOEXEC.getHandler());
            if (handler != null) {
                try {
                    ProcessTaskStepVo processTaskStepVo = new ProcessTaskStepVo();
                    processTaskStepVo.setProcessTaskId(autoexecJobProcessTaskStepVo.getProcessTaskId());
                    processTaskStepVo.setId(autoexecJobProcessTaskStepVo.getProcessTaskStepId());
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("nextStepId", nextStepVo.getId());
                    paramObj.put("action", ProcessTaskOperationType.STEP_COMPLETE.getValue());
                    processTaskStepVo.setParamObj(paramObj);
                    handler.complete(processTaskStepVo);
                    return 1;
                } catch (ProcessTaskNoPermissionException e) {
//                throw new PermissionDeniedException();
                }
            }
        }
        return 0;
    }
}
