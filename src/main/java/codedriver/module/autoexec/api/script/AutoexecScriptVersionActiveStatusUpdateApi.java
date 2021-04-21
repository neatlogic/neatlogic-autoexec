/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ScriptOperate;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionCannotActiveException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecScriptVersionActiveStatusUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/version/activestatus/update";
    }

    @Override
    public String getName() {
        return "激活或禁用脚本版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "脚本版本ID"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "1:激活;0:禁用"),
    })
    @Output({
    })
    @Description(desc = "激活或禁用脚本版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        /**
         * 激活某个版本，则把当前激活版本设为禁用，保证至多只有一个激活版本
         */
        Long versionId = jsonObj.getLong("versionId");
        Integer isActive = jsonObj.getInteger("isActive");
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        AutoexecScriptVersionVo updateVo = new AutoexecScriptVersionVo();
        if (Objects.equals(isActive, 1)) {
            if (!Objects.equals(ScriptVersionStatus.PASSED.getValue(), version.getStatus())) {
                throw new AutoexecScriptVersionCannotActiveException();
            }
            // 禁用当前激活版本
            AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(version.getScriptId());
            if (activeVersion != null) {
                updateVo.setId(activeVersion.getId());
                updateVo.setIsActive(0);
                autoexecScriptMapper.updateScriptVersion(updateVo);
            }
        }
        updateVo.setId(version.getId());
        updateVo.setIsActive(isActive);
        autoexecScriptMapper.updateScriptVersion(updateVo);

        JSONObject auditContent = new JSONObject();
        auditContent.put("version", versionId);
        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(
                version.getScriptId()
                , version.getId()
                , Objects.equals(isActive, 1) ? ScriptOperate.ACTIVE.getValue() : ScriptOperate.DISABLE.getValue()
                , auditContent);
        autoexecScriptService.audit(auditVo);
        return null;
    }


}
