/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptBaseInfoGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/baseinfo/get";
    }

    @Override
    public String getName() {
        return "查看脚本基本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
    })
    @Output({
            @Param(name = "Return", explode = AutoexecScriptVo.class, desc = "脚本内容"),
    })
    @Description(desc = "查看脚本基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(id);
        if (script == null) {
            throw new AutoexecScriptNotFoundException(id);
        }
        return script;
    }

}
