/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.autoexec.constvalue.ParamMode;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptOrToolInputParamGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/scriptortool/inputparam/get";
    }

    @Override
    public String getName() {
        return "获取工具或自定义工具输入参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "工具ID或自定义工具版本ID"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "工具或自定义工具"),
    })
    @Output({
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "inputParamList", explode = AutoexecParamVo[].class, desc = "输入参数"),
    })
    @Description(desc = "获取工具或自定义工具输入参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long id = jsonObj.getLong("id");
        String type = jsonObj.getString("type");
        String name;
        List<AutoexecParamVo> inputParamList = null;
        if (ToolType.SCRIPT.getValue().equals(type)) {
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version == null) {
                throw new AutoexecScriptVersionNotFoundException(id);
            }
            AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(version.getScriptId());
            if (script == null) {
                throw new AutoexecScriptNotFoundException(version.getScriptId());
            }
            name = script.getName();
            List<AutoexecScriptVersionParamVo> paramList = autoexecScriptMapper.getParamListByVersionId(id);
            if (CollectionUtils.isNotEmpty(paramList)) {
                inputParamList = paramList.stream()
                        .filter(o -> Objects.equals(o.getMode(), ParamMode.INPUT.getValue()))
                        .sorted(Comparator.comparing(AutoexecScriptVersionParamVo::getSort))
                        .collect(Collectors.toList());
            }
        } else {
            AutoexecToolVo tool = autoexecToolMapper.getToolById(id);
            if (tool == null) {
                throw new AutoexecToolNotFoundException(id);
            }
            name = tool.getName();
            inputParamList = tool.getInputParamList();
        }
        if (CollectionUtils.isNotEmpty(inputParamList)) {
            for (AutoexecParamVo autoexecParamVo : inputParamList) {
                autoexecService.mergeConfig(autoexecParamVo);
            }
        }
        result.put("name", name);
        result.put("inputParamList", inputParamList);
        return result;
    }

}
