package codedriver.module.autoexec.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_PROFILE_MODIFY;
import codedriver.framework.autoexec.constvalue.FromType;
import codedriver.framework.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.exception.AutoexecProfileHasBeenReferredException;
import codedriver.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/18 10:08 上午
 */
@Service
@AuthAction(action = AUTOEXEC_PROFILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecProfileDeleteApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Override
    public String getName() {
        return "保存自动化工具profile";
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
            @Param(name = "id", desc = "profile id", type = ApiParamType.LONG)
    })
    @Description(desc = "自动化工具profile保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        if (autoexecProfileMapper.checkProfileIsExists(id) == 0) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        //查询是否被引用
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        if (DependencyManager.getDependencyCount(FromType.AUTOEXEC_PROFILE_TOOL_AND_SCRIPT, id) > 0) {
            throw new AutoexecProfileHasBeenReferredException(profileVo.getName());
        }
        autoexecProfileMapper.deleteProfileById(id);
        return null;
    }
}
