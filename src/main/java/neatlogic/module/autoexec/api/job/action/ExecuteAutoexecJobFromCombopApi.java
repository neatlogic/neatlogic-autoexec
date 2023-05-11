/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.job.action;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobCanNotExecuteException;
import neatlogic.framework.autoexec.exception.AutoexecJobCannotExecuteInAdvanceException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotSupportedExecuteAndRevokeException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.autoexec.config.AutoexecConfig;
import neatlogic.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;

/**
 * @author laiwt
 * @since 2022/3/24 11:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ExecuteAutoexecJobFromCombopApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "执行作业（来自组合工具）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "执行作业（来自组合工具）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (!JobStatus.READY.getValue().equals(jobVo.getStatus()) || !UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
            throw new AutoexecJobCanNotExecuteException(jobId);
        }
        if (new Date().before(jobVo.getPlanStartTime()) && !AutoexecConfig.AUTOEXEC_JOB_IS_ALLOWED_MANUAL_TRIGGER_IN_ADVANCE()) {
            throw new AutoexecJobCannotExecuteInAdvanceException();
        }
        if (!Arrays.asList(neatlogic.framework.deploy.constvalue.CombopOperationType.PIPELINE.getValue(), CombopOperationType.COMBOP.getValue()).contains(jobVo.getOperationType())) {
            throw new AutoexecJobNotSupportedExecuteAndRevokeException();
        }
        // 执行作业、取消定时任务
        autoexecJobActionService.getJobDetailAndFireJob(jobVo);
        IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
        }
        JobObject.Builder jobObjectBuilder = new JobObject.Builder(jobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
        schedulerManager.unloadJob(jobObjectBuilder.build());
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/from/combop/execute";
    }
}
