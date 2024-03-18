/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.customtemplate;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_CUSTOMTEMPLATE_MODIFY;
import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.framework.autoexec.exception.customtemplate.CustomTemplateNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import neatlogic.module.autoexec.service.AutoexecCustomTemplateService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_CUSTOMTEMPLATE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCustomTemplateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;

    @Resource
    private AutoexecCustomTemplateService autoexecCustomTemplateService;


    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id", help = "不提供代表新增"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "common.isactive"),
            @Param(name = "template", type = ApiParamType.STRING, desc = "common.content", isRequired = true),
            @Param(name = "config", type = ApiParamType.STRING, desc = "common.config", help = "json格式的字符串")
    })
    @Description(desc = "nmaac.savecustomtemplateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        CustomTemplateVo customTemplateVo = JSONObject.toJavaObject(jsonObj, CustomTemplateVo.class);
        if (id != null) {
            if (autoexecCustomTemplateMapper.getCustomTemplateById(id) == null) {
                throw new CustomTemplateNotFoundException(id);
            }
        }
        autoexecCustomTemplateService.saveCustomTemplate(customTemplateVo);
        return null;
    }


    @Override
    public String getName() {
        return "nmaac.savecustomtemplateapi.getname";
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
