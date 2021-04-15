/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ScriptOperate;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
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
        return "删除脚本";
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
    @Description(desc = "删除脚本(此接口既可以删除脚本,也可以删除版本,两个参数二选一)")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Long versionId = jsonObj.getLong("versionId");
        if (id != null) {
            if (autoexecScriptMapper.checkScriptIsExistsById(id) == 0) {
                throw new AutoexecScriptNotFoundException(id);
            }
            // todo 不确定是否需要检查脚本是否已被引用

            List<Long> versionIdList = autoexecScriptMapper.getVersionIdListByScriptId(id);
            if (CollectionUtils.isNotEmpty(versionIdList)) {
                autoexecScriptMapper.deleteParamByVersionIdList(versionIdList);
                autoexecScriptMapper.deleteVersionByVersionIdList(versionIdList);
            }
            autoexecScriptMapper.deleteScriptLineByScriptId(id);
            autoexecScriptMapper.deleteScriptAuditByScriptId(id);
            autoexecScriptMapper.deleteScriptById(id);
        } else if (versionId != null) {
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
            if (version == null) {
                throw new AutoexecScriptVersionNotFoundException(versionId);
            }
            autoexecScriptMapper.deleteParamByVersionId(versionId);
            autoexecScriptMapper.deleteScriptLineByVersionId(versionId);
            autoexecScriptMapper.deleteVersionByVersionId(versionId);

            JSONObject auditContent = new JSONObject();
            auditContent.put("version", version.getVersion());
            AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(version.getScriptId()
                    , version.getId(), ScriptOperate.DELETE.getValue(), auditContent);
            autoexecScriptService.audit(auditVo);
        }
        return null;
    }


}
