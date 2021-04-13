/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotAnyVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.AutoexecScriptVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/get";
    }

    @Override
    public String getName() {
        return "查看脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "脚本版本ID"),
    })
    @Output({
            @Param(explode = AutoexecScriptVo[].class, desc = "脚本内容"),
    })
    @Description(desc = "查看脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (autoexecScriptMapper.checkScriptIsExistsById(id) == 0) {
            throw new AutoexecScriptNotFoundException(id);
        }
        AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(id);
        AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(id);
        if (activeVersion != null) { // 有激活版本
            script.setVersionVo(activeVersion);
            activeVersion.setParamList(autoexecScriptMapper.getParamListByVersionId(activeVersion.getId()));
            activeVersion.setLineList(autoexecScriptMapper.getLineListByVersionId(activeVersion.getId()));
            // todo 关联的流水线
        } else { // 没有激活版本，拿最新的版本
            AutoexecScriptVersionVo latestVersion = autoexecScriptMapper.getLatestVersionByScriptId(id);
            if (latestVersion == null) {
                throw new AutoexecScriptNotAnyVersionException();
            }
            script.setVersionVo(latestVersion);
            latestVersion.setParamList(autoexecScriptMapper.getParamListByVersionId(latestVersion.getId()));
            latestVersion.setLineList(autoexecScriptMapper.getLineListByVersionId(latestVersion.getId()));
            // todo 关联的流水线
        }

        return script;
    }


}
