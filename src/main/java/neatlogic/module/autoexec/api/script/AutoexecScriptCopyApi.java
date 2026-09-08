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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.*;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dependency.AutoexecScript2ScriptDependencyHandler;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptCopyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/copy";
    }

    @Override
    public String getName() {
        return "nmaa.autoexecscriptcopyapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.scriptid"),
//            @Param(name = "uk", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "term.autoexec.uniquekey"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, xss = true, desc = "common.name"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", desc = "term.autoexec.execmode"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "term.autoexec.typeid", isRequired = true),
            @Param(name = "catalogId", type = ApiParamType.LONG, desc = "term.autoexec.catalogid", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "term.autoexec.riskid"),
            @Param(name = "isLib", type = ApiParamType.INTEGER, desc = "nmaa.common.input.param.desc.islib", isRequired = true),
            @Param(name = "customTemplateId", type = ApiParamType.LONG, desc = "term.autoexec.customtemplateid"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description"),
    })
    @Output({
            @Param(type = ApiParamType.LONG, desc = "nmaa.autoexecscriptcopyapi.output.param.desc.return"),
    })
    @Description(desc = "nmaa.autoexecscriptcopyapi.getname")
    /** Copy into the requested catalog, rejecting name conflicts in that catalog. */
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecScriptVo targetScript = jsonObj.toJavaObject(AutoexecScriptVo.class);
        AutoexecScriptVo sourceScript = autoexecScriptMapper.getScriptBaseInfoById(targetScript.getId());
        if (sourceScript == null) {
            throw new AutoexecScriptNotFoundException(targetScript.getId());
        }
        targetScript.setId(null);
        targetScript.setFcu(UserContext.get().getUserUuid());
        autoexecScriptService.validateScriptBaseInfo(targetScript);
        autoexecScriptService.persistScriptBaseInfo(targetScript, true);

        // 复制所有已通过版本-->20251208改成只复制当前版本
        List<AutoexecScriptVersionVo> sourceVersionList = autoexecScriptService
                .getScriptVersionDetailListByScriptId(new AutoexecScriptVersionVo(sourceScript.getId(), ScriptVersionStatus.PASSED.getValue()));
        if (CollectionUtils.isNotEmpty(sourceVersionList)) {
            List<AutoexecScriptVersionVo> targetVersionList = new ArrayList<>();
            List<AutoexecScriptVersionParamVo> paramList = new ArrayList<>();
            List<AutoexecScriptArgumentVo> argumentList = new ArrayList<>();
            List<AutoexecScriptLineVo> lineList = new ArrayList<>();
            for (AutoexecScriptVersionVo source : sourceVersionList) {
                AutoexecScriptVersionVo target = new AutoexecScriptVersionVo();
                BeanUtils.copyProperties(source, target);
                target.setId(null);
                target.setScriptId(targetScript.getId());
                targetVersionList.add(target);
                if (CollectionUtils.isNotEmpty(source.getParamList())) {
                    source.getParamList().forEach(o -> o.setScriptVersionId(target.getId()));
                    paramList.addAll(source.getParamList());
                }
                AutoexecScriptArgumentVo argument = source.getArgument();
                if (argument != null) {
                    argument.setScriptVersionId(target.getId());
                    argumentList.add(argument);
                }
                if (CollectionUtils.isNotEmpty(source.getLineList())) {
                    source.getLineList().forEach(o -> {
                        o.setId(null);
                        o.setScriptId(targetScript.getId());
                        o.setScriptVersionId(target.getId());
                    });
                    lineList.addAll(source.getLineList());
                }
                if (paramList.size() >= 100) {
                    autoexecScriptService.batchInsertScriptVersionParamList(paramList, 100);
                    paramList.clear();
                }
                if (lineList.size() >= 100) {
                    autoexecScriptService.batchInsertScriptLineList(lineList, 100);
                    lineList.clear();
                }
                if (CollectionUtils.isNotEmpty(source.getUseLib())) {
                    autoexecScriptMapper.insertScriptVersionUseLib(target.getId(), source.getUseLib());
                    for (Long useLibId : source.getUseLib()) {
                        DependencyManager.insert(AutoexecScript2ScriptDependencyHandler.class, useLibId, target.getId());
                    }
                }
            }
            if (paramList.size() > 0) {
                autoexecScriptMapper.insertScriptVersionParamList(paramList);
            }
            if (argumentList.size() > 0) {
                autoexecScriptMapper.batchInsertVersionArgument(argumentList);
            }
            if (lineList.size() > 0) {
                autoexecScriptMapper.insertScriptLineList(lineList);
            }
            autoexecScriptMapper.batchInsertScriptVersion(targetVersionList);
        }

        return targetScript.getId();
    }


}
