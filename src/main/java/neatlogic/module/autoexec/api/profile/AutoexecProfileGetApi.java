package neatlogic.module.autoexec.api.profile;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.service.AutoexecProfileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/16 11:23 上午
 */

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
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
            @Param(name = "id", desc = "profile id", isRequired = true, type = ApiParamType.LONG)
    })
    @Output({
            @Param(explode = AutoexecProfileVo[].class, desc = "工具profile")
    })
    @Description(desc = "获取自动化工具profile接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        //获取profile关联的tool、script工具
        profileVo.setAutoexecOperationVoList(autoexecProfileMapper.getAutoexecOperationVoByProfileId(id));
        //获取profile参数
        profileVo.setProfileParamVoList(autoexecProfileService.getProfileParamListById(id));
        return profileVo;
    }
}
