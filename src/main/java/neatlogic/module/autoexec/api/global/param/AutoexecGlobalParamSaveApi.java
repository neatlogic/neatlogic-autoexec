package neatlogic.module.autoexec.api.global.param;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecGlobalParamType;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamIsNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamKeyRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamNameRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/18 6:54 下午
 */
@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecGlobalParamSaveApi extends PrivateApiComponentBase {

    @Resource
    AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Override
    public String getName() {
        return "保存自动化全局参数";
    }

    @Override
    public String getToken() {
        return "autoexec/global/param/save";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "参数id"),
            @Param(name = "key", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NUMBER_NAME, isRequired = true, desc = "参数名"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "显示名"),
            @Param(name = "type", type = ApiParamType.STRING, isRequired = true, desc = "类型"),
            @Param(name = "defaultValue", type = ApiParamType.NOAUTH, isRequired = true, desc = "值"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述")
    })
    @Description(desc = "保存自动化全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecGlobalParamVo globalParamVo = paramObj.toJavaObject(AutoexecGlobalParamVo.class);
        if (autoexecGlobalParamMapper.checkGlobalParamKeyIsRepeat(globalParamVo) > 0) {
            throw new AutoexecGlobalParamKeyRepeatException(globalParamVo.getKey());
        } else if (autoexecGlobalParamMapper.checkGlobalParamNameIsRepeat(globalParamVo) > 0) {
            throw new AutoexecGlobalParamNameRepeatException(globalParamVo.getName());
        }
        Long paramId = paramObj.getLong("id");
        if (paramId != null && autoexecGlobalParamMapper.checkGlobalParamIsExistsById(paramId) == 0) {
            throw new AutoexecGlobalParamIsNotFoundException(paramId);
        }
        // 如果参数值不以"RC4:"开头，说明密码需要加密
        if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), globalParamVo.getType()) && StringUtils.isNotBlank(globalParamVo.getDefaultValueStr())) {
            globalParamVo.setDefaultValue(RC4Util.encrypt((String) globalParamVo.getDefaultValue()));
        }
        autoexecGlobalParamMapper.insertGlobalParam(globalParamVo);
        return null;
    }

    public IValid key() {
        return value -> {
            AutoexecGlobalParamVo globalParamVo = JSON.toJavaObject(value, AutoexecGlobalParamVo.class);
            if (autoexecGlobalParamMapper.checkGlobalParamKeyIsRepeat(globalParamVo) > 0) {
                return new FieldValidResultVo(new AutoexecGlobalParamKeyRepeatException(globalParamVo.getKey()));
            }
            return new FieldValidResultVo();
        };
    }

    public IValid name() {
        return value -> {
            AutoexecGlobalParamVo globalParamVo = JSON.toJavaObject(value, AutoexecGlobalParamVo.class);
            if (autoexecGlobalParamMapper.checkGlobalParamNameIsRepeat(globalParamVo) > 0) {
                return new FieldValidResultVo(new AutoexecGlobalParamNameRepeatException(globalParamVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
