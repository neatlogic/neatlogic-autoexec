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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
        return "nmaa.autoexecscriptcheckapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parser", type = ApiParamType.STRING, isRequired = true, desc = "term.autoexec.scriptparser"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nmaa.autoexecscriptcheckapi.input.param.desc.linelist"),
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, explode = AutoexecScriptLineVo[].class, desc = "nmaa.autoexecscriptcheckapi.output.param.desc.return"),
    })
    @Description(desc = "nmaa.autoexecscriptcheckapi.getname")
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
