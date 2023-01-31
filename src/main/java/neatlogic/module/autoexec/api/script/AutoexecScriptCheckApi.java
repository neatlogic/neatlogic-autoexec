/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptLineVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptCheckHandlerNotFoundException;
import neatlogic.framework.autoexec.scriptcheck.IScriptCheckHandler;
import neatlogic.framework.autoexec.scriptcheck.ScriptCheckHandlerFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptCheckApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "autoexec/script/check";
    }

    @Override
    public String getName() {
        return "校验脚本内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parser", type = ApiParamType.STRING, isRequired = true, desc = "脚本解析器"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "脚本内容行数据列表,e.g:[{\"content\":\"#!/usr/bin/env bash\"},{\"content\":\"show_ascii_berry()\"}]"),
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, explode = AutoexecScriptLineVo[].class, desc = "经过校验后的行数据列表"),
    })
    @Description(desc = "校验脚本内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String parser = jsonObj.getString("parser");
        JSONArray lineList = jsonObj.getJSONArray("lineList");
        IScriptCheckHandler handler = ScriptCheckHandlerFactory.getHandler(parser);
        if (handler == null) {
            throw new AutoexecScriptCheckHandlerNotFoundException(parser);
        }
        List<AutoexecScriptLineVo> lineVoList = null;
        if (CollectionUtils.isNotEmpty(lineList)) {
            lineVoList = lineList.toJavaList(AutoexecScriptLineVo.class);
            handler.check(lineVoList);
        }
        return lineVoList;
    }


}
