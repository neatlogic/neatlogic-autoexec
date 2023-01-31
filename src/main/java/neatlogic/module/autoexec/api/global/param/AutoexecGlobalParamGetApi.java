package neatlogic.module.autoexec.api.global.param;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecGlobalParamType;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamIsNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/19 9:49 上午
 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecGlobalParamGetApi extends PrivateApiComponentBase {

    @Resource
    AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Override
    public String getName() {
        return "获取自动化全局参数";
    }

    @Override
    public String getToken() {
        return "autoexec/global/param/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "参数id")
    })
    @Description(desc = "获取自动化全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamById(paramId);
        if (globalParamVo == null) {
            throw new AutoexecGlobalParamIsNotFoundException(paramId);
        }
        if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), globalParamVo.getType()) && StringUtils.isNotBlank(globalParamVo.getDefaultValueStr())) {
            globalParamVo.setDefaultValue(RC4Util.decrypt(globalParamVo.getDefaultValueStr()));
        }
        return globalParamVo;
    }
}
