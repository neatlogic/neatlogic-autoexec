package neatlogic.module.autoexec.api.profile;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.exception.AutoexecProfileIsHasReferenceException;
import neatlogic.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.service.AutoexecProfileService;
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
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecProfileDeleteApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecProfileService autoexecProfileService;

    @Override
    public String getName() {
        return "删除自动化工具profile";
    }

    @Override
    public String getToken() {
        return "autoexec/profile/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "profile id", isRequired = true, type = ApiParamType.LONG)
    })
    @Description(desc = "自动化工具profile删除接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        //判断是否被依赖
        if (DependencyManager.getDependencyCount(AutoexecFromType.PROFILE, id) > 0) {
            AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
            if (profileVo == null) {
                throw new AutoexecProfileIsNotFoundException(id);
            }
            throw new AutoexecProfileIsHasReferenceException(profileVo.getName());
        }
        autoexecProfileService.deleteProfileById(paramObj.getLong("id"));
        return null;
    }
}
