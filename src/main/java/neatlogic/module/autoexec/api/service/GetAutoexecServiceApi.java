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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
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
            }
        }
        return serviceVo;
    }

}
