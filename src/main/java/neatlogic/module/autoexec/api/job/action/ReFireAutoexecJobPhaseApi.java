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
public class ReFireAutoexecJobPhaseApi extends PrivateApiComponentBase {

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
