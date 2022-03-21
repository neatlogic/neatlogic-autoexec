/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存组合工具基本信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopTemplateSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/save";
    }

    @Override
    public String getName() {
        return "保存组合工具模板基本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id"),
//            @Param(name = "uk", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "唯一名"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
//            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id"),
//            @Param(name = "owner", type = ApiParamType.STRING, minLength = 37, maxLength = 37, desc = "维护人"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "保存组合工具模板基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = jsonObj.toJavaObject(AutoexecCombopTemplateVo.class);
        if (autoexecCombopTemplateMapper.checkAutoexecCombopTemplateNameIsRepeat(autoexecCombopTemplateVo) != null) {
            throw new AutoexecCombopNameRepeatException(autoexecCombopTemplateVo.getName());
        }
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopTemplateVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(autoexecCombopTemplateVo.getTypeId());
        }
        Long id = jsonObj.getLong("id");
        if (id == null) {
            autoexecCombopTemplateVo.setOperationType(CombopOperationType.COMBOP.getValue());
            autoexecCombopTemplateVo.setConfig("{}");
            autoexecCombopTemplateMapper.insertAutoexecCombopTemplate(autoexecCombopTemplateVo);
        } else {
            AutoexecCombopTemplateVo oldAutoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopTemplateById(id);
            if (oldAutoexecCombopTemplateVo == null) {
                throw new AutoexecCombopTemplateNotFoundException(id);
            }
            AutoexecCombopConfigVo config = autoexecCombopTemplateVo.getConfig();
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            List<String> nameList = new ArrayList<>();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                String name = autoexecCombopPhaseVo.getName();
                if (nameList.contains(name)) {
                    throw new AutoexecCombopTemplatePhaseNameRepeatException(name);
                }
                nameList.add(name);
            }
//            AutoexecCombopConfigVo oldConfigVo = oldAutoexecCombopTemplateVo.getConfig();
//            /** 更新组合工具阶段列表数据时，需要保留执行目标的配置信息 **/
//            config.setExecuteConfig(oldConfigVo.getExecuteConfig());
            autoexecCombopTemplateMapper.updateAutoexecCombopTemplateById(autoexecCombopTemplateVo);
        }

        return autoexecCombopTemplateVo.getId();
    }

    public IValid name() {
        return jsonObj -> {
            AutoexecCombopTemplateVo autoexecCombopTemplateVo = jsonObj.toJavaObject(AutoexecCombopTemplateVo.class);
            if (autoexecCombopTemplateMapper.checkAutoexecCombopTemplateNameIsRepeat(autoexecCombopTemplateVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopTemplateNameRepeatException(autoexecCombopTemplateVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
