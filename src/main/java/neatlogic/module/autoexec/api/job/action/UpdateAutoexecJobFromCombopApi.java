/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobCannotUpdateException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
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
public class UpdateAutoexecJobFromCombopApi extends PrivateApiComponentBase {

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
