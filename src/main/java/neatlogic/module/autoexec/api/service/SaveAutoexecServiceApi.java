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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.constvalue.ServiceParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceAuthorityVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.dto.FormVo;
import neatlogic.framework.form.exception.FormActiveVersionNotFoundExcepiton;
import neatlogic.framework.form.exception.FormNotFoundException;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.I18nUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.dependency.AutoexecCombop2AutoexecServiceDependencyHandler;
import neatlogic.module.autoexec.dependency.Form2AutoexecServiceDependencyHandler;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecServiceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

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
//            checkRuntimeParamMapping(serviceVo);
//            String reason = autoexecServiceService.checkConfigExpired(serviceVo, true);
//            if (StringUtils.isNotBlank(reason)) {
//                serviceVo.setConfigExpired(1);
//                serviceVo.setConfigExpiredReason(reason);
//            }
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

    /**
     * 检测参数映射是否有问题
     * @param serviceVo
     */
    private void checkRuntimeParamMapping(AutoexecServiceVo serviceVo) {
        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
        if (serviceConfigVo == null) {
            return;
        }
        Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
        String formName = "";
        String formUuid = serviceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            FormVo formVo = formMapper.getFormByUuid(formUuid);
            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            if (formVersionVo == null) {
                throw new FormActiveVersionNotFoundExcepiton(formVo.getName());
            }
            List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
            if (CollectionUtils.isNotEmpty(formAttributeList)) {
                formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
            }
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(serviceVo.getCombopId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(serviceVo.getCombopId());
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(serviceVo.getCombopId());
        if (versionVo == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
        }
        autoexecCombopService.needExecuteConfig(versionVo);
        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
        List<String> list = new ArrayList<>();
        Long scenarioId = serviceConfigVo.getScenarioId();
        if (CollectionUtils.isNotEmpty(versionConfigVo.getScenarioList()) && scenarioId == null) {
            list.add("场景未设置");
        }
        ParamMappingVo roundCountMappingVo = serviceConfigVo.getRoundCount();
        if (versionVo.getNeedRoundCount()) {
            if (roundCountMappingVo == null) {
                list.add("分批数量未设置");
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        list.add("分批数量未设置");
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        list.add("未引用表单，不能映射表单属性");
                    } else if (StringUtils.isBlank((String) value)) {
                        list.add("分批数量未设置");
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            list.add("分批数量设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                        } else if (!Objects.equals(formAttributeVo.getHandler(), FormHandler.FORMNUMBER.getHandler())) {

                        }
                    }
                }
            }
        }
        ParamMappingVo protocolMappingVo = serviceConfigVo.getProtocol();
        if (versionVo.getNeedProtocol()) {
            if (protocolMappingVo == null) {
                list.add("连接协议未设置");
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        list.add("连接协议未设置");
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        list.add("未引用表单，不能映射表单属性");
                    } else if (StringUtils.isBlank((String) value)) {
                        list.add("连接协议未设置");
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            list.add("连接协议设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                        } else if (!Objects.equals(formAttributeVo.getHandler(), neatlogic.framework.cmdb.enums.FormHandler.FORMPROTOCOL.getHandler())) {

                        }
                    }
                }
            }
        }
        ParamMappingVo executeUserMappingVo = serviceConfigVo.getExecuteUser();
        if (versionVo.getNeedExecuteUser()) {
            if (executeUserMappingVo == null) {
                list.add("执行用户未设置");
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        list.add("执行用户未设置");
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        list.add("未引用表单，不能映射表单属性");
                    } else if (StringUtils.isBlank((String) value)) {
                        list.add("执行用户未设置");
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            list.add("执行用户设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                        } else if (!Objects.equals(formAttributeVo.getHandler(), FormHandler.FORMNUMBER.getHandler())) {

                        }
                    }
                }
            }
        }
        ParamMappingVo executeNodeParamMappingVo = serviceConfigVo.getExecuteNodeConfig();
        if (versionVo.getNeedExecuteNode()) {
            if (executeNodeParamMappingVo == null) {
                list.add("执行目标未设置");
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        list.add("执行目标未设置");
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        list.add("未引用表单，不能映射表单属性");
                    } else if (StringUtils.isBlank((String) value)) {
                        list.add("执行目标未设置");
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            list.add("执行目标设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                        } else if (!Objects.equals(formAttributeVo.getHandler(), neatlogic.framework.cmdb.enums.FormHandler.FORMRESOURECES.getHandler())) {

                        }
                    }
                }
            }
        }

        Map<String, AutoexecParamVo> runtimeParamMap = new HashMap<>();
        List<AutoexecParamVo> runtimeParamList = versionConfigVo.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            runtimeParamMap = runtimeParamList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
        }
        List<ParamMappingVo> runtimeParamMappingList =  serviceConfigVo.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(runtimeParamMappingList)) {
            for (ParamMappingVo runtimeParamMapping : runtimeParamMappingList) {
                String key = runtimeParamMapping.getKey();
                String name = runtimeParamMapping.getName();
                Object value = runtimeParamMapping.getValue();
                String mappingMode = runtimeParamMapping.getMappingMode();
                AutoexecParamVo runtimeParamVo = runtimeParamMap.remove(key);
                if (runtimeParamVo == null) {
                    list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“不存在");
                    continue;
                } else if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
                    list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“类型发生变化，由“" + runtimeParamMapping.getType() + "”变成“" + runtimeParamVo.getType() + "”");
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        list.add("作业参数”" + name + "(" + key + ")“未设置");
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        list.add("未引用表单，不能映射表单属性");
                    } else if (StringUtils.isBlank((String) value)) {
                        list.add("作业参数”" + name + "(" + key + ")“未设置");
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            list.add("作业参数”" + name + "(" + key + ")“设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.IS_EMPTY.getValue())) {

                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.NOT_SET_UP.getValue())) {
                    if (StringUtils.isNotBlank(formUuid)) {
                        list.add("已引用表单，作业参数”" + name + "(" + key + ")“不能不设置");
                    }
                }
//                if (StringUtils.isNotBlank(formUuid)) {
//                    if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
//                        if (value == null) {
//                            throw new AutoexecParamMappingNotMappedException(name + "(" + key + ")");
//                        }
//                    } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
//                        if (value == null) {
//                            throw new AutoexecParamMappingNotMappedException(name + "(" + key + ")");
//                        }
//                    }
//                } else {
//                    if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
//                        if (value == null) {
//                            throw new AutoexecParamMappingNotMappedException(name + "(" + key + ")");
//                        }
//                    }
//                }
            }
        }
        if (MapUtils.isNotEmpty(runtimeParamMap)) {
            for (Map.Entry<String, AutoexecParamVo> entry : runtimeParamMap.entrySet()) {
                AutoexecParamVo runtimeParamVo = entry.getValue();
                list.add("作业参数“" + runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")“未映射");
            }
        }
//        if (CollectionUtils.isNotEmpty(list)) {
//            return String.join("；", list);
//        }
    }
}
