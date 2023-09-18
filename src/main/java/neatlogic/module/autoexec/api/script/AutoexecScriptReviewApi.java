/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionCannotReviewException;
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
        return "nmaas.autoexecscriptreviewapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "common.versionid"),
            @Param(name = "action", type = ApiParamType.ENUM, rule = "pass,reject", isRequired = true, desc = "nmaas.autoexecscriptreviewapi.input.param.desc"),
            @Param(name = "content", type = ApiParamType.STRING, desc = "common.content")
    })
    @Output({
    })
    @Description(desc = "nmaas.autoexecscriptreviewapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long versionId = jsonObj.getLong("versionId");
        String action = jsonObj.getString("action");
        String content = jsonObj.getString("content");
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
//        if (autoexecScriptMapper.getScriptLockById(version.getScriptId()) == null) {
//            throw new AutoexecScriptNotFoundException(version.getScriptId());
//        }
        if (!Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
            throw new AutoexecScriptVersionCannotReviewException();
        }
//        boolean isPass = Objects.equals(ScriptAction.PASS.getValue(), action);
//        AutoexecScriptVersionVo updateVo = new AutoexecScriptVersionVo();
//        updateVo.setId(versionId);
//        updateVo.setReviewer(UserContext.get().getUserUuid());
//        updateVo.setLcu(UserContext.get().getUserUuid());
//        // 如果审批通过，那么该版本成为当前激活版本，生成最新版本号
//        if (isPass) {
//            updateVo.setStatus(ScriptVersionStatus.PASSED.getValue());
//            Integer maxVersion = autoexecScriptMapper.getMaxVersionByScriptId(version.getScriptId());
//            updateVo.setVersion(maxVersion != null ? maxVersion + 1 : 1);
//            updateVo.setIsActive(1);
//            // 禁用之前的激活版本
//            AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(version.getScriptId());
//            if (activeVersion != null) {
//                activeVersion.setIsActive(0);
//                autoexecScriptMapper.updateScriptVersion(activeVersion);
//            }
//        } else {
//            updateVo.setStatus(ScriptVersionStatus.REJECTED.getValue());
//        }
//        autoexecScriptMapper.updateScriptVersion(updateVo);
//
//        JSONObject auditContent = new JSONObject();
//        auditContent.put("version", version.getVersion());
//        if (StringUtils.isNotBlank(content)) {
//            auditContent.put("content", content);
//        }
//        AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(version.getScriptId(), version.getId(), action, auditContent);
//        autoexecScriptService.audit(auditVo);
        autoexecScriptService.reviewVersion(version, action, content);
        return null;
    }


}
