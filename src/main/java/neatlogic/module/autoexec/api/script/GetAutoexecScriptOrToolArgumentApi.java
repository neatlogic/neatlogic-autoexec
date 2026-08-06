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
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptHasNoActiveVersionException;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecScriptOrToolArgumentApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/scriptortool/argument/get";
    }

    @Override
    public String getName() {
        return "nmaa.getautoexecscriptortoolargumentapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "nmaa.getautoexecscriptortoolargumentapi.input.param.desc.id"),
            @Param(name = "scriptId", type = ApiParamType.LONG, desc = "term.autoexec.scriptid"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "nmaa.getautoexecscriptortoolargumentapi.input.param.desc.type"),
    })
    @Output({
            @Param(name = "argument", explode = AutoexecScriptArgumentVo.class, desc = "term.autoexec.freeparam"),
    })
    @Description(desc = "nmaa.getautoexecscriptortoolargumentapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id =jsonObj.getLong("id");
        if (ToolType.SCRIPT.getValue().equals(jsonObj.getString("type"))) {
            Long scriptId = jsonObj.getLong("scriptId");
            if (id != null) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version == null) {
                    throw new AutoexecScriptVersionNotFoundException(id);
                }
                return autoexecScriptMapper.getArgumentByVersionId(id);
            } else if (scriptId != null) {
                AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
                if (script == null) {
                    throw new AutoexecScriptNotFoundException(scriptId);
                }
                AutoexecScriptVersionVo version = autoexecScriptMapper.getActiveVersionByScriptId(scriptId);
                if (version == null) {
                    throw new AutoexecScriptHasNoActiveVersionException(script.getName());
                }
                return autoexecScriptMapper.getArgumentByVersionId(version.getId());
            } else {
                throw new ParamNotExistsException("id", "scriptId");
            }
        }else{
            if (id == null) {
                throw new ParamNotExistsException("id");
            }
            AutoexecToolVo tool = autoexecToolMapper.getToolById(id);
            if (tool == null) {
                throw new AutoexecToolNotFoundException(id);
            }
            return tool.getArgument();
        }
    }

}
