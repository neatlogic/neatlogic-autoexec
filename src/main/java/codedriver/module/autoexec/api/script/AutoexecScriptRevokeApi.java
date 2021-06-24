/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.ScriptAction;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionIsNotSubmittedException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptRevokeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/revoke";
    }

    @Override
    public String getName() {
        return "脚本撤回提交审核";
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
    @Description(desc = "脚本撤回提交审核")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long versionId = jsonObj.getLong("versionId");
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionIdForUpdate(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        if (!Objects.equals(version.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            throw new AutoexecScriptVersionIsNotSubmittedException();
        }

        AutoexecScriptVersionVo vo = new AutoexecScriptVersionVo();
        vo.setId(version.getId());
        vo.setStatus(ScriptVersionStatus.DRAFT.getValue());
        vo.setLcu(UserContext.get().getUserUuid());
        autoexecScriptMapper.updateScriptVersion(vo);

        JSONObject auditContent = new JSONObject();
        auditContent.put("version", version.getVersion());
        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(version.getScriptId(), version.getId(), ScriptAction.REVOKE.getValue(), auditContent);
        autoexecScriptService.audit(auditVo);

        return result;
    }


}
