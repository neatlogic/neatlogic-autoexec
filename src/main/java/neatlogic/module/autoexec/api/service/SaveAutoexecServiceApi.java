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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceAuthorityVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecServiceIsNotCatalogException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNameIsRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormVo;
import neatlogic.framework.form.exception.FormNotFoundException;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.dependency.AutoexecCombop2AutoexecServiceDependencyHandler;
import neatlogic.module.autoexec.dependency.Form2AutoexecServiceDependencyHandler;
import neatlogic.module.autoexec.service.AutoexecServiceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SERVICE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAutoexecServiceApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private FormMapper formMapper;

    @Resource
    AutoexecServiceService autoexecServiceService;

    @Override
    public String getToken() {
        return "autoexec/service/save";
    }

    @Override
    public String getName() {
        return "保存服务目录信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "服务id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "服务名"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "service,catalog", isRequired = true, desc = "服务/目录"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"),
            @Param(name = "authorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "授权列表"),
            @Param(name = "parentId", type = ApiParamType.LONG, isRequired = true, desc = "父级id"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具id"),
            @Param(name = "formUuid", type = ApiParamType.STRING, desc = "表单uuid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "配置信息"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "说明")
    })
    @Description(desc = "保存服务目录信息接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecServiceVo serviceVo = paramObj.toJavaObject(AutoexecServiceVo.class);
        if (autoexecServiceMapper.checkAutoexecServiceNameIsRepeat(serviceVo) > 0) {
            throw new AutoexecServiceNameIsRepeatException(serviceVo.getName());
        }

        if (StringUtils.equals(serviceVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
            if (!paramObj.containsKey("config")) {
                throw new ParamNotExistsException("config");
            }
            if (!paramObj.containsKey("combopId")) {
                throw new ParamNotExistsException("combopId");
            }
            String formUuid = serviceVo.getFormUuid();
            if (StringUtils.isNotEmpty(formUuid)) {
                FormVo formVo = formMapper.getFormByUuid(formUuid);
                if (formVo == null) {
                    throw new FormNotFoundException(formUuid);
                }
            }
            autoexecServiceService.checkConfigExpired(serviceVo, true);
        }
        Long id = paramObj.getLong("id");
        Long parentId = paramObj.getLong("parentId");
        if (parentId != null && parentId != 0L) {
            AutoexecServiceVo parent = autoexecServiceMapper.getAutoexecServiceById(parentId);
            if (parent == null) {
                throw new AutoexecServiceNotFoundException(parentId);
            }
            if (StringUtils.equals(parent.getType(), AutoexecServiceType.SERVICE.getValue())) {
               throw new AutoexecServiceIsNotCatalogException(parent.getName());
            }
        } else {
            parentId = 0L;
        }
        if (id == null) {
            int lft = LRCodeManager.beforeAddTreeNode("autoexec_service", "id", "parent_id", parentId);
            serviceVo.setLft(lft);
            serviceVo.setRht(lft + 1);
            autoexecServiceMapper.insertAutoexecService(serviceVo);
        } else {
            if (StringUtils.equals(serviceVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
                DependencyManager.delete(AutoexecCombop2AutoexecServiceDependencyHandler.class, serviceVo.getId());
                DependencyManager.delete(Form2AutoexecServiceDependencyHandler.class, serviceVo.getId());
            }
            autoexecServiceMapper.deleteServiceAuthorityListByServiceId(serviceVo.getId());
            autoexecServiceMapper.updateServiceById(serviceVo);
        }
        if (paramObj.getString("type").equals("service")) {
            serviceVo.setConfigExpired(0);
            autoexecServiceMapper.insertAutoexecServiceConfig(serviceVo);
            Long combopId = serviceVo.getCombopId();
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
            if (autoexecCombopVo != null) {
                JSONObject dependencyConfig = new JSONObject();
                dependencyConfig.put("combopId", combopId);
                dependencyConfig.put("combopName", autoexecCombopVo.getName());
                dependencyConfig.put("serviceId", serviceVo.getId());
                dependencyConfig.put("serviceName", serviceVo.getName());
                DependencyManager.insert(AutoexecCombop2AutoexecServiceDependencyHandler.class, combopId, serviceVo.getId(), dependencyConfig);
            } else {
                throw new AutoexecCombopNotFoundException(combopId);
            }
            String formUuid = serviceVo.getFormUuid();
            if (StringUtils.isNotEmpty(formUuid)) {
                FormVo formVo = formMapper.getFormByUuid(formUuid);
                if (formVo != null) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("formUuid", formVo.getUuid());
                    dependencyConfig.put("formName", formVo.getName());
                    dependencyConfig.put("serviceId", serviceVo.getId());
                    dependencyConfig.put("serviceName", serviceVo.getName());
                    DependencyManager.insert(Form2AutoexecServiceDependencyHandler.class, formUuid, serviceVo.getId(), dependencyConfig);
                } else {
                    throw new FormNotFoundException(formUuid);
                }
            }
        }
        List<AutoexecServiceAuthorityVo> authorityVoList = new ArrayList<>();
        for (String authority : serviceVo.getAuthorityList()) {
            String[] split = authority.split("#");
            if (GroupSearch.getGroupSearch(split[0]) != null) {
                AutoexecServiceAuthorityVo authorityVo = new AutoexecServiceAuthorityVo();
                authorityVo.setServiceId(serviceVo.getId());
                authorityVo.setType(split[0]);
                authorityVo.setUuid(split[1]);
                authorityVoList.add(authorityVo);
            }
        }
        if (CollectionUtils.isNotEmpty(authorityVoList)) {
            autoexecServiceMapper.insertAutoexecServiceAuthorityList(authorityVoList);
        }
        return serviceVo.getId();
    }

    public IValid name() {
        return value -> {
            AutoexecServiceNodeVo vo = JSON.toJavaObject(value, AutoexecServiceNodeVo.class);
            if (autoexecServiceMapper.checkAutoexecServiceNameIsRepeat(vo) > 0) {
                return new FieldValidResultVo(new AutoexecServiceNameIsRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
