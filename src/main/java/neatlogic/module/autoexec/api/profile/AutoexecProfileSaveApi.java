package neatlogic.module.autoexec.api.profile;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecProfileNameRepeatsException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.service.AutoexecProfileService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/18 10:08 上午
 */
@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecProfileSaveApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecProfileService autoexecProfileService;

    @Override
    public String getName() {
        return "保存自动化工具profile";
    }

    @Override
    public String getToken() {
        return "autoexec/profile/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "profile id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "profile 名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "fromSystemId", type = ApiParamType.LONG, isRequired = true, desc = "所属系统id"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "工具参数"),
            @Param(name = "autoexecOperationVoList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "关联的工具和脚本列表")
    })
    @Output({
    })
    @Description(desc = "自动化工具profile保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramProfileId = paramObj.getLong("id");
        AutoexecProfileVo profileVo = JSON.toJavaObject(paramObj, AutoexecProfileVo.class);
        if (paramProfileId != null && autoexecProfileMapper.checkProfileIsExists(paramProfileId) == 0) {
            throw new AutoexecProfileIsNotFoundException(paramProfileId);
        }
        if (autoexecProfileMapper.checkProfileNameIsRepeats(profileVo) > 0) {
            throw new AutoexecProfileNameRepeatsException(profileVo.getName());
        }
        //保存profile、profile参数、profile参数值引用全局参数的关系、profile引用tool、script的关系
        autoexecProfileService.saveProfile(profileVo);
        return null;
    }

    public IValid name() {
        return value -> {
            AutoexecProfileVo vo = JSON.toJavaObject(value, AutoexecProfileVo.class);
            if (autoexecProfileMapper.checkProfileNameIsRepeats(vo) > 0) {
                return new FieldValidResultVo(new AutoexecProfileNameRepeatsException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
