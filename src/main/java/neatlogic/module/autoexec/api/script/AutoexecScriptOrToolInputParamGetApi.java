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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ParamMode;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptHasNoActiveVersionException;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptOrToolInputParamGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/scriptortool/inputparam/get";
    }

    @Override
    public String getName() {
        return "获取工具或自定义工具输入参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "执行对象ID"),
            @Param(name = "operationType", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "执行对象类型"),
    })
    @Output({
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "inputParamList", explode = AutoexecParamVo[].class, desc = "输入参数"),
    })
    @Description(desc = "获取工具或自定义工具输入参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long operationId = jsonObj.getLong("operationId");
        String operationType = jsonObj.getString("operationType");
        String name;
        List<AutoexecParamVo> inputParamList = null;
        if (operationId == null) {
            throw new ParamNotExistsException("operationId");
        }
        if (operationType == null) {
            throw new ParamNotExistsException("operationType");
        }
        if (ToolType.SCRIPT.getValue().equals(operationType)) {
            AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(operationId);
            if (script == null) {
                throw new AutoexecScriptNotFoundException(operationId);
            }
            name = script.getName();
            AutoexecScriptVersionVo version = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
            if (version == null) {
                throw new AutoexecScriptHasNoActiveVersionException(name);
            }
            List<AutoexecScriptVersionParamVo> paramList = autoexecScriptMapper.getParamListByVersionId(version.getId());
            if (CollectionUtils.isNotEmpty(paramList)) {
                inputParamList = paramList.stream()
                        .filter(o -> Objects.equals(o.getMode(), ParamMode.INPUT.getValue()))
                        .sorted(Comparator.comparing(AutoexecScriptVersionParamVo::getSort))
                        .collect(Collectors.toList());
            }
        } else {
            AutoexecToolVo tool = autoexecToolMapper.getToolById(operationId);
            if (tool == null) {
                throw new AutoexecToolNotFoundException(operationId);
            }
            name = tool.getName();
            inputParamList = tool.getInputParamList();
        }
        if (CollectionUtils.isNotEmpty(inputParamList)) {
            for (AutoexecParamVo autoexecParamVo : inputParamList) {
                autoexecService.mergeConfig(autoexecParamVo);
            }
        }
        result.put("id", operationId);
        result.put("operationId", operationId);
        result.put("operationType", operationType);
        result.put("name", name);
        result.put("inputParamList", inputParamList);
        return result;
    }

}
