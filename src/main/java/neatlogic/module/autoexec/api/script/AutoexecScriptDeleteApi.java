/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.ScriptAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionCannotDeleteException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionHasBeenActivedException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecScriptDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/delete";
    }

    @Override
    public String getName() {
        return "删除脚本或版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID(两个参数二选一)"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本ID"),
    })
    @Output({
    })
    @Description(desc = "删除脚本或版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Long versionId = jsonObj.getLong("versionId");
        if (id != null) { // 删除脚本
            if (autoexecScriptMapper.getScriptLockById(id) == null) {
                throw new AutoexecScriptNotFoundException(id);
            }
            autoexecScriptService.deleteScriptById(id);
        } else if (versionId != null) { // 删除版本
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionIdForUpdate(versionId);
            if (version == null) {
                throw new AutoexecScriptVersionNotFoundException(versionId);
            }
            if (autoexecScriptMapper.getScriptLockById(version.getScriptId()) == null) {
                throw new AutoexecScriptNotFoundException(version.getScriptId());
            }
            if (Objects.equals(version.getIsActive(), 1)) {
                throw new AutoexecScriptVersionHasBeenActivedException();
            }
            boolean hasOnlyOneVersion = autoexecScriptMapper.getVersionCountByScriptId(version.getScriptId()) == 1;
            autoexecScriptMapper.deleteParamByVersionId(versionId);
            autoexecScriptMapper.deleteArgumentByVersionId(versionId);
            autoexecScriptMapper.deleteScriptLineByVersionId(versionId);
            autoexecScriptMapper.deleteVersionByVersionId(versionId);
            // 只剩一个版本时，直接删除整个脚本
            if (hasOnlyOneVersion) {
                autoexecScriptMapper.deleteScriptById(version.getScriptId());
            } else {
                JSONObject auditContent = new JSONObject();
                auditContent.put("version", version.getVersion());
                AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(version.getScriptId()
                        , version.getId(), ScriptAction.DELETE.getValue(), auditContent);
                autoexecScriptService.audit(auditVo);
            }
        }
        return null;
    }

    /**
     * 检查脚本是否只有一个版本
     *
     * @return
     */
    public IValid id() {
        return value -> {
            Long id = value.getLong("id");
            if (autoexecScriptMapper.checkScriptIsExistsById(id) == 0) {
                return new FieldValidResultVo(new AutoexecScriptNotFoundException(id));
            }
            if (autoexecScriptMapper.getVersionCountByScriptId(id) == 1) {
                return new FieldValidResultVo(new AutoexecScriptVersionCannotDeleteException());
            }
            return new FieldValidResultVo();
        };
    }


}
