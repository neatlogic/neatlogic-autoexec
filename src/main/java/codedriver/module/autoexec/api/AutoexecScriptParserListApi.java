/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptParserListApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "autoexec/script/parser/list";
    }

    @Override
    public String getName() {
        return "获取脚本解析器列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, desc = "脚本解析器列表"),
    })
    @Description(desc = "获取脚本解析器列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return null;
    }


}
