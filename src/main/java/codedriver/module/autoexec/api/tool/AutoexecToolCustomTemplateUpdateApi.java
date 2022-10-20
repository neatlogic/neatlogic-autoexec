/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.exception.AutoexecToolNotFoundException;
import codedriver.framework.autoexec.exception.customtemplate.CustomTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecToolCustomTemplateUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/customtemplate/update";
    }

    @Override
    public String getName() {
        return "更新工具绑定的自定义模版";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "工具ID"),
            @Param(name = "customTemplateId", type = ApiParamType.LONG, isRequired = true, desc = "自定义模版ID"),
    })
    @Output({
    })
    @Description(desc = "更新工具绑定的自定义模版")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Long customTemplateId = jsonObj.getLong("customTemplateId");
        if (autoexecToolMapper.checkToolExistsById(id) == 0) {
            throw new AutoexecToolNotFoundException(id);
        }
        if (autoexecCustomTemplateMapper.checkCustomTemplateIsExistsById(customTemplateId) == 0) {
            throw new CustomTemplateNotFoundException(customTemplateId);
        }
        AutoexecToolVo vo = new AutoexecToolVo();
        vo.setId(id);
        vo.setCustomTemplateId(customTemplateId);
        autoexecToolMapper.updateCustomTemplate(vo);
        return null;
    }


}
