/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.dependency;

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
            lastName = "版本" + versionVo.getVersion() + "(" + versionVo.getTitle() + ")";
            urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?scriptId=${DATA.scriptId}&status=${DATA.status}";
        } else {
            urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?versionId=${DATA.versionId}&status=${DATA.status}";
            if (Objects.equals(versionVo.getStatus(), ScriptVersionStatus.PASSED.getValue())) {
                lastName = "版本" + versionVo.getVersion() + "(" + versionVo.getTitle() + ")";
            } else {
                lastName = ScriptVersionStatus.getText(versionVo.getStatus()) + "(" + versionVo.getTitle() + ")";
                if (Objects.equals(versionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
                    urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/review-detail?scriptId=${DATA.scriptId}&versionId=${DATA.versionId}";
                }
            }
        }

        List<String> pathList = new ArrayList<>();
        pathList.add("自定义工具库(" + scriptVo.getName() + ")");
        return new DependencyInfoVo(Long.valueOf(dependencyVo.getTo()), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCRIPT;
    }
}
