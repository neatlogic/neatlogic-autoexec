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

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.autoexec.exception.customtemplate.CustomTemplateNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
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
            @Param(name = "customTemplateId", type = ApiParamType.LONG, desc = "自定义模版ID"),
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
        if (customTemplateId != null && autoexecCustomTemplateMapper.checkCustomTemplateIsExistsById(customTemplateId) == 0) {
            throw new CustomTemplateNotFoundException(customTemplateId);
        }
        AutoexecToolVo vo = new AutoexecToolVo();
        vo.setId(id);
        vo.setCustomTemplateId(customTemplateId);
        autoexecToolMapper.updateCustomTemplate(vo);
        return null;
    }


}
