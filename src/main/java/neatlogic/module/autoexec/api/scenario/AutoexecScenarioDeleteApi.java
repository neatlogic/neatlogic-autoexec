package neatlogic.module.autoexec.api.scenario;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/19 2:16 下午
 */
@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecScenarioDeleteApi extends PrivateApiComponentBase {

    @Resource
    AutoexecScenarioMapper autoexecScenarioMapper;

    @Override
    public String getName() {
        return "删除场景";
    }

    @Override
    public String getToken() {
        return "autoexec/scenario/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "场景id")
    })
    @Description(desc = "删除场景接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        AutoexecScenarioVo paramScenarioVo = autoexecScenarioMapper.getScenarioById(paramId);
        if (paramScenarioVo == null) {
            throw new AutoexecScenarioIsNotFoundException(paramId);
        }
        //经产品确认，场景的删除不做判断，在前端页面已告知是否正在被引用，二次确认是否删除
        DependencyManager.deleteByFrom(AutoexecFromType.SCENARIO, paramId);
        autoexecScenarioMapper.deleteScenarioById(paramId);
        return null;
    }
}
