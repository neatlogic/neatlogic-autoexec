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
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.constvalue.ScriptAction;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionCannotActiveException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptVersionSwitchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/version/switch";
    }

    @Override
    public String getName() {
        return "nmaa.autoexecscriptversionswitchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.scriptversionid"),
    })
    @Output({
    })
    @Description(desc = "nmaa.autoexecscriptversionswitchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long versionId = jsonObj.getLong("versionId");
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        if (autoexecScriptMapper.getScriptLockById(version.getScriptId()) == null) {
            throw new AutoexecScriptNotFoundException(version.getScriptId());
        }
        if (!Objects.equals(ScriptVersionStatus.PASSED.getValue(), version.getStatus())) {
            throw new AutoexecScriptVersionCannotActiveException();
        }
        AutoexecScriptVersionVo updateVo = new AutoexecScriptVersionVo();
        // 禁用当前激活版本
        AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(version.getScriptId());
        updateVo.setId(activeVersion.getId());
        updateVo.setIsActive(0);
        autoexecScriptMapper.updateScriptVersion(updateVo);
        updateVo.setId(version.getId());
        updateVo.setIsActive(1);
        autoexecScriptMapper.updateScriptVersion(updateVo);

        JSONObject auditContent = new JSONObject();
        auditContent.put("oldVersion", activeVersion.getVersion());
        auditContent.put("newVersion", version.getVersion());
        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(
                version.getScriptId()
                , version.getId()
                , ScriptAction.SWITCH_VERSION.getValue()
                , auditContent);
        autoexecScriptService.audit(auditVo);
        return null;
    }


}
