/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.constvalue.ScriptOperate;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionCannotSubmitException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
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
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
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
            @Param(name = "isReviewable", type = ApiParamType.ENUM, rule = "0,1", desc = "是否能审批(1:能;0:不能)"),
    })
    @Description(desc = "脚本提交审核")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long versionId = jsonObj.getLong("versionId");
        AutoexecScriptVersionVo version = autoexecScriptService.getScriptVersionDetailByVersionId(versionId);
        AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(version.getScriptId());
        if (script == null) {
            throw new AutoexecScriptNotFoundException(version.getScriptId());
        }
        autoexecScriptService.validateScriptBaseInfo(script);
        if (!Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                && !Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus())) {
            throw new AutoexecScriptVersionCannotSubmitException();
        }

        // todo 校验脚本内容

        AutoexecScriptVersionVo vo = new AutoexecScriptVersionVo();
        vo.setId(version.getId());
        vo.setStatus(ScriptVersionStatus.SUBMITTED.getValue());
        vo.setLcu(UserContext.get().getUserUuid());
        autoexecScriptMapper.updateScriptVersion(vo);

        JSONObject auditContent = new JSONObject();
        auditContent.put("version", version.getVersion());
        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(script.getId(), version.getId(), ScriptOperate.SUBMIT.getValue(), auditContent);
        autoexecScriptService.audit(auditVo);

        int isReviewable = 0;
        if (AuthActionChecker.check(AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())) {
            isReviewable = 1;
        }
        result.put("isReviewable", isReviewable);
        return result;
    }


}
