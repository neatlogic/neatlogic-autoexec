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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecParallelPolicy;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceAuthorityVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.service.AutoexecServiceNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.service.AutoexecServiceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecServiceApi extends PrivateApiComponentBase {

    @Resource
    AutoexecServiceMapper autoexecServiceMapper;

    @Resource
    AutoexecServiceService autoexecServiceService;

    @Override
    public String getToken() {
        return "autoexec/service/get";
    }

    @Override
    public String getName() {
        return "nmpac.cataloggetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id")
    })
    @Output({
            @Param(explode = AutoexecServiceVo.class, desc = "term.autoexec.serviceinfo")
    })
    @Description(desc = "nmpac.cataloggetapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecServiceVo serviceVo = autoexecServiceMapper.getAutoexecServiceById(id);
        if (serviceVo == null) {
            throw new AutoexecServiceNotFoundEditTargetException(id);
        }
        //兼容老数据
        if (serviceVo.getConfig() != null && serviceVo.getConfig().getRoundCount() != null && serviceVo.getConfig().getRoundCount().getValue() != null && serviceVo.getConfig().getParallelPolicy() == null) {
            ParamMappingVo parallelPolicy = new ParamMappingVo();
            parallelPolicy.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            parallelPolicy.setValue(AutoexecParallelPolicy.ROUND_COUNT.getValue());
            serviceVo.getConfig().setParallelPolicy(parallelPolicy);
        }
        List<AutoexecServiceAuthorityVo> authorityVoList = autoexecServiceMapper.getAutoexecServiceAuthorityListByServiceId(id);
        if (CollectionUtils.isNotEmpty(authorityVoList)) {
            List<String> authorityList = new ArrayList<>();
            authorityVoList.forEach(e -> authorityList.add(e.getType() + "#" + e.getUuid()));
            serviceVo.setAuthorityList(authorityList);
        }
        if (Objects.equals(serviceVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
            JSONArray reasonList = autoexecServiceService.checkConfigExpired(serviceVo, false);
            if (CollectionUtils.isNotEmpty(reasonList)) {
                serviceVo.setConfigExpired(1);
                JSONObject reasonObj = new JSONObject();
                reasonObj.put("reasonList", reasonList);
                serviceVo.setConfigExpiredReason(reasonObj);
                autoexecServiceMapper.updateServiceConfigExpiredById(serviceVo);
            } else {
                serviceVo.setConfigExpired(0);
                serviceVo.setConfigExpiredReason(null);
                autoexecServiceMapper.updateServiceConfigExpiredById(serviceVo);
            }
        }
        return serviceVo;
    }

}
