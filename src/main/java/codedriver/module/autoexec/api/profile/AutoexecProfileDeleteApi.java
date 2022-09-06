package codedriver.module.autoexec.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.exception.AutoexecProfileIsHasReferenceException;
import codedriver.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.module.autoexec.service.AutoexecProfileService;
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
