/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
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
public class AutoexecScriptVersionCompareApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/version/compare";
    }

    @Override
    public String getName() {
        return "脚本版本对比";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "currentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "当前版本ID"),
            @Param(name = "targetVersionId", type = ApiParamType.LONG, isRequired = true, desc = "目标版本ID"),
    })
    @Output({
            @Param(name = "currentScript", explode = AutoexecScriptVo[].class, desc = "当前版本脚本"),
            @Param(name = "targetScript", explode = AutoexecScriptVo[].class, desc = "目标版本脚本"),
    })
    @Description(desc = "脚本版本对比")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return null;
    }


}
