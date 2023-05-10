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

package neatlogic.module.autoexec.api.job.exec;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseOperationNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseOperationScriptApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/operation/script/get";
    }

    @Override
    public String getName() {
        return "获取作业剧本操作脚本内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.STRING, desc = "作业操作id（opName_opId）", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否获取最新版本的脚本（1：是，0：不是，默认是）"),
    })
    @Output({
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, desc = "脚本内容行列表")
    })
    @Description(desc = "获取作业剧本操作脚本内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long opId = jsonObj.getLong("operationId");
        AutoexecJobPhaseOperationVo jobPhaseOperationVo = autoexecJobMapper.getJobPhaseOperationByOperationId(opId);
        if (jobPhaseOperationVo == null) {
            throw new AutoexecJobPhaseOperationNotFoundException(opId.toString());
        }

        if (jsonObj.getInteger("isActive") == null || jsonObj.getInteger("isActive") == 1) {

            //获取最新版本的脚本
            AutoexecScriptVersionVo scriptVersionVoOld = autoexecScriptMapper.getVersionByVersionId(jobPhaseOperationVo.getVersionId());
            if (scriptVersionVoOld == null) {
                throw new AutoexecScriptNotFoundException(jobPhaseOperationVo.getName() + ":" + jobPhaseOperationVo.getVersionId());
            }
            AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(scriptVersionVoOld.getScriptId());
            if (scriptVersionVo == null) {
                throw new AutoexecScriptVersionHasNoActivedException(jobPhaseOperationVo.getName());
            }
            return autoexecScriptMapper.getLineListByVersionId(scriptVersionVo.getId());
        } else {
            //获取对应的版本脚本
            return autoexecScriptMapper.getLineListByVersionId(jobPhaseOperationVo.getVersionId());
        }
    }
}
