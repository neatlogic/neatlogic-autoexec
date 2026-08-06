/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobGroupVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 *
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ReFireAutoexecJobPhaseApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmaa.refireautoexecjobphaseapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "phaseId", type = ApiParamType.LONG, desc = "nmaa.refireautoexecjobphaseapi.input.param.desc.phaseid", isRequired = true),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "refireResetAll,refireAll", desc = "nmaa.refireautoexecjobphaseapi.input.param.desc.type")
    })
    @Output({
    })
    @Description(desc = "nmaa.refireautoexecjobphaseapi.getname")
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
        jobVo.setExecutePhase(phaseVo);
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
