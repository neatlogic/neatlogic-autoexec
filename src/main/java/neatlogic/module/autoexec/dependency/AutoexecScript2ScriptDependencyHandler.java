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

package neatlogic.module.autoexec.dependency;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AutoexecScript2ScriptDependencyHandler extends DefaultDependencyHandlerBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        Long versionId = Long.valueOf(dependencyVo.getTo());
        AutoexecScriptVersionVo versionVo = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (versionVo == null) {
            return null;
        }
        AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(versionVo.getScriptId());
        if (scriptVo == null) {
            return null;
        }
        String urlFormat = "";
        String lastName = "";
        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("scriptId", versionVo.getScriptId());
        dependencyInfoConfig.put("versionId", versionVo.getId());
        dependencyInfoConfig.put("status", versionVo.getStatus());
        if (Objects.equals(versionVo.getIsActive(), 1)) {
            lastName = $.t("nmar.dependency.versionprefix") + versionVo.getVersion() + "(" + versionVo.getTitle() + ")";
            urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?scriptId=${DATA.scriptId}&status=${DATA.status}";
        } else {
            urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?versionId=${DATA.versionId}&status=${DATA.status}";
            if (Objects.equals(versionVo.getStatus(), ScriptVersionStatus.PASSED.getValue())) {
                lastName = $.t("nmar.dependency.versionprefix") + versionVo.getVersion() + "(" + versionVo.getTitle() + ")";
            } else {
                lastName = ScriptVersionStatus.getText(versionVo.getStatus()) + "(" + versionVo.getTitle() + ")";
                if (Objects.equals(versionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
                    urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/review-detail?scriptId=${DATA.scriptId}&versionId=${DATA.versionId}";
                }
            }
        }

        List<String> pathList = new ArrayList<>();
        pathList.add($.t("nmar.dependency.scriptlibraryprefix") + scriptVo.getName() + ")");
        return new DependencyInfoVo(Long.valueOf(dependencyVo.getTo()), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCRIPT;
    }
}
