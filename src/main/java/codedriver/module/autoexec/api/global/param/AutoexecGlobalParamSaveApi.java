package codedriver.module.autoexec.api.global.param;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.constvalue.AutoexecGlobalParamType;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.exception.AutoexecGlobalParamDisplayNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecGlobalParamIsNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecGlobalParamNameRepeatException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
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
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[a-zA-Z0-9_\\.]+$", isRequired = true, desc = "参数名"),
            @Param(name = "displayName", type = ApiParamType.STRING, isRequired = true, desc = "显示名"),
            @Param(name = "type", type = ApiParamType.STRING, isRequired = true, desc = "类型"),
            @Param(name = "value", type = ApiParamType.STRING, isRequired = true, desc = "参数值"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述")
    })
    @Description(desc = "保存自动化全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecGlobalParamVo globalParamVo = paramObj.toJavaObject(AutoexecGlobalParamVo.class);
        if (autoexecGlobalParamMapper.checkGlobalParamNameIsRepeat(globalParamVo) > 0) {
            throw new AutoexecGlobalParamNameRepeatException(globalParamVo.getName());
        } else if (autoexecGlobalParamMapper.checkGlobalParamDisplayNameIsRepeat(globalParamVo) > 0) {
            throw new AutoexecGlobalParamDisplayNameRepeatException(globalParamVo.getName());
        }
        Long paramId = paramObj.getLong("id");
        if (paramId != null && autoexecGlobalParamMapper.checkGlobalParamIsExistsById(paramId) == 0) {
            throw new AutoexecGlobalParamIsNotFoundException(paramId);
        }
        // 如果参数值不以"RC4:"开头，说明密码需要加密
        if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), globalParamVo.getType()) && StringUtils.isNotBlank(globalParamVo.getValue()) && !globalParamVo.getValue().startsWith(CiphertextPrefix.RC4.getValue())) {
            globalParamVo.setValue(CiphertextPrefix.RC4.getValue() + RC4Util.encrypt(globalParamVo.getValue()));
        }
        autoexecGlobalParamMapper.insertGlobalParam(globalParamVo);
        return null;
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

    public IValid displayName() {
        return value -> {
            AutoexecGlobalParamVo globalParamVo = JSON.toJavaObject(value, AutoexecGlobalParamVo.class);
            if (autoexecGlobalParamMapper.checkGlobalParamDisplayNameIsRepeat(globalParamVo) > 0) {
                return new FieldValidResultVo(new AutoexecGlobalParamDisplayNameRepeatException(globalParamVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}