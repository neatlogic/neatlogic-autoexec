package codedriver.module.autoexec.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_PROFILE_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecProfileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/16 11:23 上午
 */
@AuthAction(action = AUTOEXEC_PROFILE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecProfileGetApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecProfileService autoexecProfileService;

    @Override
    public String getName() {
        return "获取自动化工具profile";
    }

    @Override
    public String getToken() {
        return "autoexec/profile/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "profile id", type = ApiParamType.LONG)
    })
    @Output({
            @Param(explode = AutoexecProfileVo[].class, desc = "工具profile")
    })
    @Description(desc = "获取自动化工具profile接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo != null) {
            //获取profile参数
            profileVo.setInputParamList(autoexecProfileService.getProfileParamById(id));
            return profileVo;
        }
        return null;
    }
}
