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
        return "删除服务目录信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "服务id")
    })
    @Description(desc = "删除服务目录信息接口")
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
