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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecServiceHasBeenReferredException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.dependency.AutoexecCombop2AutoexecServiceDependencyHandler;
import neatlogic.module.autoexec.dependency.Form2AutoexecServiceDependencyHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@AuthAction(action = AUTOEXEC_SERVICE_MANAGE.class)
@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAutoexecServiceApi extends PrivateApiComponentBase {

    @Resource
    AutoexecServiceMapper autoexecServiceMapper;

    @Override
    public String getToken() {
        return "autoexec/service/delete";
    }

    @Override
    public String getName() {
        return "nmaa.deleteautoexecserviceapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.serviceid")
    })
    @Description(desc = "nmaa.deleteautoexecserviceapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecServiceNodeVo serviceNodeVo = autoexecServiceMapper.getAutoexecServiceNodeById(id);
        if (serviceNodeVo == null) {
            throw new AutoexecServiceNotFoundException(id);
        }
        if (StringUtils.equals(serviceNodeVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
            int count = autoexecServiceMapper.getAutoexecServiceCountByParentId(id);
            if (count > 0) {
                throw new AutoexecServiceHasBeenReferredException(serviceNodeVo.getName());
            }
        }
        LRCodeManager.beforeDeleteTreeNode("autoexec_service", "id", "parent_id", id);
        if (StringUtils.equals(serviceNodeVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
            DependencyManager.delete(AutoexecCombop2AutoexecServiceDependencyHandler.class, id);
            DependencyManager.delete(Form2AutoexecServiceDependencyHandler.class, id);
            autoexecServiceMapper.deleteAutoexecServiceUserByServiceId(id);
            autoexecServiceMapper.deleteAutoexecServiceAuthorityByServiceId(id);
        }
        autoexecServiceMapper.deleteAutoexecServiceById(id);
        return null;
    }
}
