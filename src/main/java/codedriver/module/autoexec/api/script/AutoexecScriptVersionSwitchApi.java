/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.constvalue.ScriptAction;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionCannotActiveException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
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
        return "切换脚本当前版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "脚本版本ID"),
    })
    @Output({
    })
    @Description(desc = "切换脚本当前版本")
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
