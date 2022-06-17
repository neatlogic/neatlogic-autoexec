/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobGroupVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

/**
 *
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobPhaseReFireApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "重跑作业阶段";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "phaseId", type = ApiParamType.LONG, desc = "作业阶段id", isRequired = true),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "refireResetAll,refireAll", desc = "重跑类型：   重置并重跑所有：refireResetAll；重跑所有：refireAll,默认重置并重跑所有")
    })
    @Output({
    })
    @Description(desc = "重跑作业阶段接口")
    @ResubmitInterval(value = 4)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long phaseId = jsonObj.getLong("phaseId");
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(phaseId);
        if(phaseVo==null){
            throw new AutoexecJobPhaseNotFoundException(phaseId.toString());
        }
        AutoexecJobGroupVo jobGroupVo = autoexecJobMapper.getJobGroupById(phaseVo.getGroupId());
        phaseVo.setJobGroupVo(jobGroupVo);
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(phaseVo.getJobId());
        jobVo.setExecuteJobGroupVo(jobGroupVo);
        jobVo.setExecuteJobPhaseList(Collections.singletonList(phaseVo));
        jobVo.setCurrentPhase(phaseVo);
        jobVo.setAction(JobAction.RESET_REFIRE.getValue());
        if(jsonObj.containsKey("type")){
            jobVo.setAction(jsonObj.getString("type"));
        }
        jobVo.setIsNoFireNext(1);
        IAutoexecJobActionHandler refireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.REFIRE_PHASE.getValue());
        return refireAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/refire";
    }
}
