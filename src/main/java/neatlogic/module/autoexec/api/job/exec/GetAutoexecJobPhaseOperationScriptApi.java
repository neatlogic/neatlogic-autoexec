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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
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
        return "nmaa.getautoexecjobphaseoperationscriptapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.STRING, desc = "term.autoexec.operationid", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "nmaa.getautoexecjobphaseoperationscriptapi.input.param.desc.isactive"),
    })
    @Output({
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.linelist")
    })
    @Description(desc = "nmaa.getautoexecjobphaseoperationscriptapi.getname")
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
