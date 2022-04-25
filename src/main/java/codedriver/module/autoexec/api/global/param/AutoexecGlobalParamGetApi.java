package codedriver.module.autoexec.api.global.param;

import codedriver.framework.autoexec.constvalue.AutoexecGlobalParamType;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.exception.AutoexecGlobalParamIsNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/19 9:49 上午
 */
@Service

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
        if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), globalParamVo.getType()) && StringUtils.isNotBlank(globalParamVo.getValue()) && globalParamVo.getValue().startsWith(CiphertextPrefix.RC4.getValue())) {
            globalParamVo.setValue(RC4Util.decrypt(globalParamVo.getValue().substring(4)));
        }
        return globalParamVo;
    }
}
