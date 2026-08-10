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

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecOperationIndexAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import neatlogic.module.autoexec.service.AutoexecOperationChangeDispatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecScriptBaseInfoSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Resource
    private AutoexecOperationChangeDispatcher operationChangeDispatcher;

    @Override
    public String getToken() {
        return "autoexec/script/baseinfo/save";
    }

    @Override
    public String getName() {
        return "nmaa.autoexecscriptbaseinfosaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.scriptid"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, xss = true, desc = "common.name"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", desc = "term.autoexec.execmode"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "term.autoexec.typeid", isRequired = true),
            @Param(name = "catalogId", type = ApiParamType.LONG, desc = "term.autoexec.catalogid", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "term.autoexec.riskid"),
            @Param(name = "isLib", type = ApiParamType.INTEGER, desc = "nmaa.common.input.param.desc.islib", isRequired = true),
            @Param(name = "customTemplateId", type = ApiParamType.LONG, desc = "term.autoexec.customtemplateid"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description"),
            @Param(name = "defaultProfileId", type = ApiParamType.LONG, desc = "nmaa.autoexecscriptbaseinfosaveapi.input.param.desc.defaultprofileid"),
    })
    @Output({
    })
    @Description(desc = "nmaa.autoexecscriptbaseinfosaveapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        if (autoexecScriptMapper.checkScriptIsExistsById(scriptVo.getId()) == 0) {
            throw new AutoexecScriptNotFoundException(scriptVo.getId());
        }
        autoexecScriptService.validateScriptBaseInfo(scriptVo);
        autoexecScriptMapper.updateScriptBaseInfo(scriptVo);
        operationChangeDispatcher.notifyAfterCommit("script", scriptVo.getId(), AutoexecOperationIndexAction.UPSERT);
        return null;
    }

}
