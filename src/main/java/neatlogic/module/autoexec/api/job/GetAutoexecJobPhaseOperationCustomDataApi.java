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

package neatlogic.module.autoexec.api.job;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseOperationCustomDataApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取阶段工具个性化数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id"),
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "工具id"),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobPhaseId = paramObj.getLong("jobPhaseId");
        Long operationId = paramObj.getLong("operationId");
        AutoexecJobPhaseOperationVo operation = autoexecJobMapper.getMaxSortJobPhaseOperationByPhaseIdAndOperationId(jobPhaseId, operationId);
        if (operation != null) {
            paramObj.put("jobPhaseOperationId", operation.getId());
            paramObj.put("operationName", operation.getName());
            AutoexecJobVo jobVo = new AutoexecJobVo();
            jobVo.setActionParam(paramObj);
            jobVo.setAction(JobAction.GET_OPERATION_CUSTOM_DATA.getValue());
            IAutoexecJobActionHandler action = AutoexecJobActionHandlerFactory.getAction(JobAction.GET_OPERATION_CUSTOM_DATA.getValue());
            return action.doService(jobVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/phase/operation/customdata/get";
    }
}
