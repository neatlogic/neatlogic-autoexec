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

package neatlogic.module.autoexec.api.job.action.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeOperationStatusVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseNodeInputParamApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmaa.getautoexecjobphasenodeinputparamapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.jobphaseid"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "term.cmdb.resourceid")
    })
    @Output({
            @Param(explode = AutoexecJobPhaseNodeOperationStatusVo[].class, desc = "term.autoexec.jobphasenodetoolstatuslist"),
    })
    @Description(desc = "nmaa.getautoexecjobphasenodeinputparamapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setId(paramObj.getLong("jobId"));
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.GET_NODE_OPERATION_INPUT_PARAM.getValue());
        IAutoexecJobActionHandler getNodeOperationInputParamAction = AutoexecJobActionHandlerFactory.getAction(JobAction.GET_NODE_OPERATION_INPUT_PARAM.getValue());
        return getNodeOperationInputParamAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/operation/input/param/get";
    }


}
