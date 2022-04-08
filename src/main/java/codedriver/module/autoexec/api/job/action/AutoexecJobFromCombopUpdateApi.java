/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobCannotUpdateException;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author laiwt
 * @since 2022/3/23 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobFromCombopUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "作业修改（来自组合工具）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业ID"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "auto,manual", desc = "触发方式"),
    })
    @Output({
    })
    @Description(desc = "作业修改（来自组合工具）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Date planStartTime = jsonObj.getDate("planStartTime");
        String triggerType = jsonObj.getString("triggerType");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (planStartTime == null && StringUtils.isBlank(triggerType)) {
            throw new ParamNotExistsException("planStartTime", "triggerType");
        }
        // 执行用户才能修改计划时间和触发方式
        if (!JobStatus.READY.getValue().equals(jobVo.getStatus()) || !UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
            throw new AutoexecJobCannotUpdateException(jobId);
        }
        AutoexecJobVo updateVo = new AutoexecJobVo(jobId, planStartTime, triggerType);
        if (planStartTime == null) {
            updateVo.setPlanStartTime(jobVo.getPlanStartTime());
        }
        if (StringUtils.isBlank(triggerType)) {
            updateVo.setTriggerType(jobVo.getTriggerType());
            triggerType = jobVo.getTriggerType();
        }
        autoexecJobMapper.updateJobPlanStartTimeAndTriggerTypeById(updateVo);
        IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
        JobObject builder = new JobObject.Builder(jobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid()).build();
        if (JobTriggerType.AUTO.getValue().equals(triggerType)) {
            reloadJob(jobHandler, builder);
        } else if (JobTriggerType.MANUAL.getValue().equals(triggerType)) {
            schedulerManager.unloadJob(builder);
        }
        return null;

    }

    private void reloadJob(IJob jobHandler, JobObject builder) {
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
        }
        schedulerManager.unloadJob(builder);
        jobHandler.reloadJob(builder);
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/update";
    }
}
