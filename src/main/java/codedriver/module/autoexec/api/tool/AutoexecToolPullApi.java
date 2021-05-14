/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.exception.AutoexecRiskNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecToolPullApi extends PublicApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/pull";
    }

    @Override
    public String getName() {
        return "拉取内置工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "opName", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, desc = "工具名称"),
            @Param(name = "opType", type = ApiParamType.ENUM, rule = "runner,target,runner_target", isRequired = true, desc = "执行方式"),
            @Param(name = "typeName", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, desc = "工具分类名称"),
            @Param(name = "riskName", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, desc = "操作级别名称"),
            @Param(name = "interpreter", type = ApiParamType.ENUM, rule = "python,vbs,shell,perl,powershell,bat,xml", isRequired = true, desc = "解析器"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
    })
    @Output({
    })
    @Description(desc = "拉取内置工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String opName = jsonObj.getString("opName");
        String opType = jsonObj.getString("opType");
        String typeName = jsonObj.getString("typeName");
        String riskName = jsonObj.getString("riskName");
        String interpreter = jsonObj.getString("interpreter");
        String description = jsonObj.getString("description");
        Long typeId = autoexecTypeMapper.getTypeIdByName(typeName);
        Long riskId = autoexecRiskMapper.getRiskIdByName(riskName);
        if (typeId == null) {
            throw new AutoexecTypeNotFoundException(typeName);
        }
        if (riskId == null) {
            throw new AutoexecRiskNotFoundException(riskName);
        }
        AutoexecToolVo vo = new AutoexecToolVo();
        vo.setName(opName);
        vo.setExecMode(opType);
        vo.setInterpreter(interpreter);
        vo.setTypeId(typeId);
        vo.setRiskId(riskId);
        Integer isActive = autoexecToolMapper.getActiveStatusByName(opName);
        vo.setIsActive(isActive != null ? isActive : 1);
        vo.setDescription(description);
        autoexecToolMapper.replaceTool(vo);

        return null;
    }


}
