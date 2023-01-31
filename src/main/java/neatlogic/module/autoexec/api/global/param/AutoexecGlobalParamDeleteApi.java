package neatlogic.module.autoexec.api.global.param;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamHasBeenReferredException;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamIsNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/19 2:10 下午
 */
@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecGlobalParamDeleteApi extends PrivateApiComponentBase {

    @Resource
    AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Override
    public String getName() {
        return "删除自动化全局参数";
    }

    @Override
    public String getToken() {
        return "autoexec/global/param/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "参数id")
    })
    @Description(desc = "删除自动化全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamById(paramId);
        if (globalParamVo == null) {
            throw new AutoexecGlobalParamIsNotFoundException(paramId);
        }
        //判断是否被profile、组合工具引用
        if (DependencyManager.getDependencyCount(AutoexecFromType.GLOBAL_PARAM, globalParamVo.getKey()) > 0) {
            throw new AutoexecGlobalParamHasBeenReferredException(globalParamVo.getName());
        }
        autoexecGlobalParamMapper.deleteGlobalParamById(paramId);
        return null;
    }
}
