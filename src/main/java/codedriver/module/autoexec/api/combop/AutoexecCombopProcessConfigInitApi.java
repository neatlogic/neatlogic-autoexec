/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * @author linbq
 * @since 2021/9/2 18:34
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopProcessConfigInitApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "autoexec/combop/processconfig/init";
    }

    @Override
    public String getName() {
        return "组合工具流程自动化节点配置初始化";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "runtimeParamList", type = ApiParamType.JSONARRAY, desc = "运行参数列表"),
            @Param(name = "executeParamList", type = ApiParamType.JSONARRAY, desc = "执行参数列表")
    })
    @Description(desc = "查询组合工具授权信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("runtimeParamList", new ArrayList<>());
        resultObj.put("executeParamList", new ArrayList<>());
        return resultObj;
    }
}
