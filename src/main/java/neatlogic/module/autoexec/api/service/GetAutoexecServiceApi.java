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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.util.I18nUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceAuthorityVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecServiceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SERVICE_MANAGE.class)
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
        return "获取服务目录信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "服务id")
    })
    @Output({
            @Param(explode = AutoexecServiceVo.class, desc = "服务目录信息")
    })
    @Description(desc = "获取服务目录信息接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecServiceVo serviceVo = autoexecServiceMapper.getAutoexecServiceById(id);
        if (serviceVo == null) {
            throw new AutoexecServiceNotFoundException(id);
        }
        List<AutoexecServiceAuthorityVo> authorityVoList = autoexecServiceMapper.getAutoexecServiceAuthorityListByServiceId(id);
        if (CollectionUtils.isNotEmpty(authorityVoList)) {
            List<String> authorityList = new ArrayList<>();
            authorityVoList.forEach(e -> authorityList.add(e.getType() + "#" + e.getUuid()));
            serviceVo.setAuthorityList(authorityList);
        }
        if (Objects.equals(serviceVo.getType(), AutoexecServiceType.SERVICE.getValue()) && Objects.equals(serviceVo.getConfigExpired(), 0)) {
            String reason = autoexecServiceService.checkConfigExpired(serviceVo);
            if (StringUtils.isNotBlank(reason)) {
                serviceVo.setConfigExpired(1);
                serviceVo.setConfigExpiredReason(reason);
                autoexecServiceMapper.updateServiceConfigExpiredById(serviceVo);
            }
        }
        return serviceVo;
    }

}
