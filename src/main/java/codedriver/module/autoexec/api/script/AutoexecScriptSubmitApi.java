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
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptHasSubmittedVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionCannotSubmitException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
public class AutoexecScriptSubmitApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/submit";
    }

    @Override
    public String getName() {
        return "脚本提交审核";
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
    @Description(desc = "脚本提交审核")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long versionId = jsonObj.getLong("versionId");
        AutoexecScriptVersionVo version = autoexecScriptService.getScriptVersionDetailByVersionId(versionId);
        Long scriptId = version.getScriptId();
        AutoexecScriptVo script = autoexecScriptMapper.getScriptLockById(scriptId);
        if (script == null) {
            throw new AutoexecScriptNotFoundException(scriptId);
        }
        Long catalogId = autoexecScriptMapper.getAutoexecCatalogIdByScriptId(scriptId);
        if (Objects.isNull(catalogId)) {
            throw new AutoexecCatalogNotFoundException(catalogId);
        }
        script.setCatalogId(catalogId);
        autoexecScriptService.validateScriptBaseInfo(script);
        if (!Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                && !Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus())) {
            throw new AutoexecScriptVersionCannotSubmitException();
        }
        if (autoexecScriptMapper.checkScriptHasSubmittedVersionByScriptId(script.getId()) > 0) {
            throw new AutoexecScriptHasSubmittedVersionException();
        }

        // todo 校验脚本内容

        AutoexecScriptVersionVo vo = new AutoexecScriptVersionVo();
        vo.setId(version.getId());
        vo.setStatus(ScriptVersionStatus.SUBMITTED.getValue());
        vo.setLcu(UserContext.get().getUserUuid());
        autoexecScriptMapper.updateScriptVersion(vo);

        JSONObject auditContent = new JSONObject();
        auditContent.put("version", version.getVersion());
        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(script.getId(), version.getId(), ScriptAction.SUBMIT.getValue(), auditContent);
        autoexecScriptService.audit(auditVo);

        return result;
    }


}
