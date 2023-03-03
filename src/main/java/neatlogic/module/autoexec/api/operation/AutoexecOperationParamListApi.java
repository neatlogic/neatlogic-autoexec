package neatlogic.module.autoexec.api.operation;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecService;
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
public class AutoexecOperationParamListApi extends PrivateApiComponentBase {

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
