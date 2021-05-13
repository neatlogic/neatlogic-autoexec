/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionNumberListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/version/number/list";
    }

    @Override
    public String getName() {
        return "获取脚本版本号列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scriptId", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, explode = ValueTextVo[].class, desc = "版本号列表"),
    })
    @Description(desc = "获取脚本版本号列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long scriptId = jsonObj.getLong("scriptId");
        if (autoexecScriptMapper.checkScriptIsExistsById(scriptId) == 0) {
            throw new AutoexecScriptNotFoundException(scriptId);
        }
        return autoexecScriptMapper.getVersionNumberListByScriptId(scriptId);
    }

}
