/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobCanNotExecuteException;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobNotSupportedExecuteAndRevokeException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author laiwt
 * @since 2022/3/24 11:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobExecuteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "执行作业";
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
    @Description(desc = "执行作业")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (!JobStatus.READY.getValue().equals(jobVo.getStatus())) {
            throw new AutoexecJobCanNotExecuteException(jobId);
        }
        if (!CombopOperationType.COMBOP.getValue().equals(jobVo.getOperationType())) {
            throw new AutoexecJobNotSupportedExecuteAndRevokeException();
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(jobVo.getOperationId());
        }
        if (autoexecCombopService.checkOperableButton(autoexecCombopVo, CombopAuthorityAction.EXECUTE)) {
            // 执行作业、取消定时任务
            autoexecJobActionService.getJobDetailAndFireJob(jobVo);
            IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
            if (jobHandler == null) {
                throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
            }
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(jobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
            schedulerManager.unloadJob(jobObjectBuilder.build());
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/execute";
    }
}
