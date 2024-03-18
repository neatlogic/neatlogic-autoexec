/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.ScriptAction;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptHasSubmittedVersionException;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionCannotSubmitException;
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
        if (catalogId == null) {
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
