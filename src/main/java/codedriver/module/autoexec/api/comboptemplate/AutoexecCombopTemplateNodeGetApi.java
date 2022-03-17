/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 查询组合工具模板执行目标信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopTemplateNodeGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/node/get";
    }

    @Override
    public String getName() {
        return "查询组合工具模板执行目标信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopTemplateId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "执行用户"),
            @Param(name = "whenToSpecify", type = ApiParamType.ENUM, rule = "now,runtime", desc = "执行目标指定时机，现在指定/运行时再指定"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标信息"),
            @Param(name = "protocolId", type = ApiParamType.LONG, desc = "协议id")

    })
    @Description(desc = "查询组合工具模板执行目标信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopTemplateId = jsonObj.getLong("combopTemplateId");
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopById(combopTemplateId);
        if (autoexecCombopTemplateVo == null) {
            throw new AutoexecCombopTemplateNotFoundException(combopTemplateId);
        }
        return autoexecCombopTemplateVo.getConfig().getExecuteConfig();
    }

}
