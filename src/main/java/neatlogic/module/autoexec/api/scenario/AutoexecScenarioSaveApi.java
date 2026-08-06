package neatlogic.module.autoexec.api.scenario;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScenarioRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/15 11:45 上午
 */
@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScenarioSaveApi extends PrivateApiComponentBase {

    @Resource
    AutoexecScenarioMapper autoexecScenarioMapper;

    @Override
    public String getName() {
        return "nmaa.autoexecscenariosaveapi.getname";
    }

    @Override
    public String getToken() {
        return "autoexec/scenario/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description")
    })
    @Description(desc = "nmaa.autoexecscenariosaveapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScenarioVo paramScenarioVo = paramObj.toJavaObject(AutoexecScenarioVo.class);
        if (autoexecScenarioMapper.checkScenarioNameIsRepeat(paramScenarioVo) > 0) {
            throw new AutoexecScenarioRepeatException(paramScenarioVo.getName());
        }
        Long paramId = paramObj.getLong("id");
        if (paramId != null && autoexecScenarioMapper.checkScenarioIsExistsById(paramId) == 0) {
            throw new AutoexecScenarioIsNotFoundException(paramId);
        }
        autoexecScenarioMapper.insertScenario(paramScenarioVo);
        return null;
    }

    public IValid name() {
        return value -> {
            AutoexecScenarioVo paramScenarioVo = JSON.toJavaObject(value, AutoexecScenarioVo.class);
            if (autoexecScenarioMapper.checkScenarioNameIsRepeat(paramScenarioVo) > 0) {
                return new FieldValidResultVo(new AutoexecScenarioRepeatException(paramScenarioVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
