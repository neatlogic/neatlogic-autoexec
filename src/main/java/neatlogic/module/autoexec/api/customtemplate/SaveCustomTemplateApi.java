/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            if (autoexecCustomTemplateMapper.getCustomTemplateById(id) == null) {
                throw new CustomTemplateNotFoundException(id);
            }
        }
        autoexecCustomTemplateService.saveCustomTemplate(customTemplateVo);
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
