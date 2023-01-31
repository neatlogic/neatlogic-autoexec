package neatlogic.module.autoexec.api.global.param;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
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
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecGlobalParamUpdateForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Override
    public String getName() {
        return "更新自动化全局参数";
    }

    @Override
    public String getToken() {
        return "autoexec/global/param/update/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({
            @Param(name = "key", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NUMBER_NAME, isRequired = true, desc = "参数名"),
            @Param(name = "defaultValue", type = ApiParamType.NOAUTH, isRequired = true, desc = "值"),
    })
    @Description(desc = "更新自动化全局参数,用于autoexec")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String key = paramObj.getString("key");
        String defaultValue = paramObj.getString("defaultValue");
        AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(key);
        if (globalParamVo == null) {
            throw new AutoexecGlobalParamIsNotFoundException(key);
        }

        // 如果参数值不以"RC4:"开头，说明密码需要加密
        if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), globalParamVo.getType()) && StringUtils.isNotBlank(globalParamVo.getDefaultValueStr())) {
            globalParamVo.setDefaultValue(RC4Util.encrypt(defaultValue));
        } else {
            globalParamVo.setDefaultValue(defaultValue);
        }
        autoexecGlobalParamMapper.insertGlobalParam(globalParamVo);
        return null;
    }
}
