/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptOutputParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/outputparam/list";
    }

    @Override
    public String getName() {
        return "获取脚本出参列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scriptIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "脚本ID列表"),
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, desc = "出参列表"),
            @Param(explode = AutoexecScriptVersionParamVo.class),
    })
    @Description(desc = "获取脚本出参列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray scriptIdList = jsonObj.getJSONArray("scriptIdList");
        // 根据脚本ID列表查询各自激活版本的出参列表
        return autoexecScriptMapper.getOutputParamListByScriptIdList(scriptIdList.toJavaList(Long.class));
    }


}
