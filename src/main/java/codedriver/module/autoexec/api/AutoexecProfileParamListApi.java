package codedriver.module.autoexec.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/23 5:45 下午
 */
@AuthAction(action = AUTOEXEC_BASE.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecProfileParamListApi extends PrivateApiComponentBase {

    @Resource
    AutoexecService autoexecService;

    @Override
    public String getName() {
        return "获取自动化工具参数列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/operation/param/list";
    }

    @Input({
            @Param(name = "autoexecOperationVoList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "关联的工具和脚本列表")
    })
    @Output({
    })
    @Description(desc = "获取自动化工具参数列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return autoexecService.getAutoexecOperationParamVoList(paramObj.getJSONArray("autoexecOperationVoList").toJavaList(AutoexecOperationVo.class));
    }
}
