/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
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
        return "获取工具或自定义工具自由参数";
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
            @Param(name = "argument", explode = AutoexecScriptArgumentVo.class, desc = "自由参数"),
    })
    @Description(desc = "获取工具或自定义工具自由参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id =jsonObj.getLong("id");
        if (ToolType.SCRIPT.getValue().equals(jsonObj.getString("type"))) {
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version == null) {
                throw new AutoexecScriptVersionNotFoundException(id);
            }
            return autoexecScriptMapper.getArgumentByVersionId(id);
        }else{
            AutoexecToolVo tool = autoexecToolMapper.getToolById(id);
            if (tool == null) {
                throw new AutoexecToolNotFoundException(id);
            }
            return tool.getArgument();
        }
    }

}
