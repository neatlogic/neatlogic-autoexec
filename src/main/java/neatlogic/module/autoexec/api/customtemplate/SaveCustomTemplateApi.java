/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id", help = "nmaa.savecustomtemplateapi.input.param.help.id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "common.isactive"),
            @Param(name = "template", type = ApiParamType.STRING, desc = "common.content", isRequired = true),
            @Param(name = "config", type = ApiParamType.STRING, desc = "common.config", help = "nmaa.savecustomtemplateapi.input.param.help.config")
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
