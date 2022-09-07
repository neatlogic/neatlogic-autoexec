/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.customtemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_CUSTOMTEMPLATE_MODIFY;
import codedriver.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import codedriver.framework.autoexec.exception.customtemplate.CustomTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_CUSTOMTEMPLATE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCustomTemplateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;


    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"),
            @Param(name = "template", type = ApiParamType.STRING, desc = "模板内容", isRequired = true),
            @Param(name = "config", type = ApiParamType.STRING, desc = "配置内容，json格式的字符串")
    })
    @Description(desc = "保存自定义模板接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        CustomTemplateVo customTemplateVo = JSONObject.toJavaObject(jsonObj, CustomTemplateVo.class);
        if (id != null) {
            if (autoexecCustomTemplateMapper.getCustomTemplateById(id) != null) {
                autoexecCustomTemplateMapper.updateCustomTemplate(customTemplateVo);
            } else {
                throw new CustomTemplateNotFoundException(id);
            }
        } else {
            autoexecCustomTemplateMapper.insertCustomTemplate(customTemplateVo);
        }
        return null;
    }


    @Override
    public String getName() {
        return "保存自定义模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/customtemplate/save";
    }
}
