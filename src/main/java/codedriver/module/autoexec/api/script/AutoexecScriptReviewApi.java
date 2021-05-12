/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ScriptOperate;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionCannotReviewException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptReviewApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/review";
    }

    @Override
    public String getName() {
        return "审核脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "脚本版本ID"),
            @Param(name = "action", type = ApiParamType.ENUM, rule = "pass,reject", isRequired = true, desc = "通过，驳回"),
            @Param(name = "content", type = ApiParamType.STRING, desc = "驳回原因")
    })
    @Output({
    })
    @Description(desc = "审核脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long versionId = jsonObj.getLong("versionId");
        String action = jsonObj.getString("action");
        String content = jsonObj.getString("content");
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        if (!Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
            throw new AutoexecScriptVersionCannotReviewException();
        }
        AutoexecScriptVersionVo updateVo = new AutoexecScriptVersionVo();
        updateVo.setId(versionId);
        updateVo.setStatus(Objects.equals(ScriptOperate.PASS.getValue(), action)
                ? ScriptVersionStatus.PASSED.getValue() : ScriptVersionStatus.REJECTED.getValue());
        updateVo.setReviewer(UserContext.get().getUserUuid());
        updateVo.setLcu(UserContext.get().getUserUuid());
        autoexecScriptMapper.updateScriptVersion(updateVo);

        JSONObject auditContent = new JSONObject();
        auditContent.put("version", version.getVersion());
        if (StringUtils.isNotBlank(content)) {
            auditContent.put("content", content);
        }
        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(version.getScriptId(), version.getId(), action, auditContent);
        autoexecScriptService.audit(auditVo);
        return null;
    }


}
