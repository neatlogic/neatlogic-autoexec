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

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.constvalue.ServiceParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.dto.FormVo;
import neatlogic.framework.form.exception.FormActiveVersionNotFoundExcepiton;
import neatlogic.framework.form.exception.FormNotFoundException;
import neatlogic.framework.util.I18nUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoexecServiceServiceImpl implements AutoexecServiceService {

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private FormMapper formMapper;

    @Override
    public String checkConfigExpired(AutoexecServiceVo serviceVo, boolean throwException) {
//        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(serviceVo.getCombopId());
//        if (autoexecCombopVo == null) {
//            return I18nUtils.getMessage("exception.autoexec.autoexeccombopnotfoundexception", serviceVo.getCombopId());
//        }
//        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(serviceVo.getCombopId());
//        if (versionVo == null) {
//            return I18nUtils.getMessage("exception.autoexec.autoexeccombopactiveversionnotfoundexception", autoexecCombopVo.getName());
//        }
//        autoexecCombopService.needExecuteConfig(versionVo);
//        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
//        List<AutoexecParamVo> runtimeParamList = versionConfigVo.getRuntimeParamList();
//        Map<String, AutoexecParamVo> runtimeParamMap = runtimeParamList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
//
//        List<String> list = new ArrayList<>();
//        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
//        Long scenarioId = serviceConfigVo.getScenarioId();
//        if (CollectionUtils.isNotEmpty(versionConfigVo.getScenarioList()) && scenarioId == null) {
//            list.add("场景未设置");
//        }
//        ParamMappingVo roundCountMappingVo = serviceConfigVo.getRoundCount();
//        if (versionVo.getNeedRoundCount()) {
//            if (roundCountMappingVo == null) {
//                list.add("分批数量未设置");
//            } else {
//                String mappingMode = roundCountMappingVo.getMappingMode();
//                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
//
//                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.IS_EMPTY.getValue())) {
//
//                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
//
//                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.NOT_SET_UP.getValue())) {
//
//                }
//            }
//        }
//        ParamMappingVo protocolMappingVo = serviceConfigVo.getProtocol();
//        if (versionVo.getNeedProtocol()) {
//            if (protocolMappingVo == null) {
//                list.add("连接协议未设置");
//            }
//        }
//        ParamMappingVo executeUserMappingVo = serviceConfigVo.getExecuteUser();
//        if (versionVo.getNeedExecuteUser()) {
//            if (executeUserMappingVo == null) {
//                list.add("执行用户未设置");
//            }
//        }
//        ParamMappingVo executeNodeParamMappingVo = serviceConfigVo.getExecuteNodeConfig();
//        if (versionVo.getNeedExecuteNode()) {
//            if (executeNodeParamMappingVo == null) {
//                list.add("执行目标未设置");
//            }
//        }
//        List<ParamMappingVo> runtimeParamMappingList = serviceConfigVo.getRuntimeParamList();
//        for (ParamMappingVo runtimeParamMapping : runtimeParamMappingList) {
//            String key = runtimeParamMapping.getKey();
//            AutoexecParamVo runtimeParamVo = runtimeParamMap.remove(key);
//            if (runtimeParamVo == null) {
//                list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“不存在");
//                continue;
//            }
//            if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
//                list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“类型发生变化，由“" + runtimeParamMapping.getType() + "”变成“" + runtimeParamVo.getType() + "”");
//            }
//        }
//        if (MapUtils.isNotEmpty(runtimeParamMap)) {
//            for (Map.Entry<String, AutoexecParamVo> entry : runtimeParamMap.entrySet()) {
//                AutoexecParamVo runtimeParamVo = entry.getValue();
//                list.add("作业参数“" + runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")“未映射");
//            }
//        }
//        if (CollectionUtils.isNotEmpty(list)) {
//            return String.join("；", list);
//        }
        Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
        String formName = "";
        String formUuid = serviceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            FormVo formVo = formMapper.getFormByUuid(formUuid);
            if (formVo == null) {
                if (throwException) {
                    throw new FormNotFoundException(formUuid);
                } else {

                }
            }
            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            if (formVersionVo == null) {
                if (throwException) {
                    throw new FormActiveVersionNotFoundExcepiton(formVo.getName());
                } else {

                }
            }
            List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
            if (CollectionUtils.isNotEmpty(formAttributeList)) {
                formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
            }
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(serviceVo.getCombopId());
        if (autoexecCombopVo == null) {
            if (throwException) {
                throw new AutoexecCombopNotFoundException(serviceVo.getCombopId());
            } else {

            }
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(serviceVo.getCombopId());
        if (versionVo == null) {
            if (throwException) {
                throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
            } else {

            }
        }
        autoexecCombopService.needExecuteConfig(versionVo);
        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
        List<String> list = new ArrayList<>();
        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
        Long scenarioId = serviceConfigVo.getScenarioId();
        if (CollectionUtils.isNotEmpty(versionConfigVo.getScenarioList()) && scenarioId == null) {
            if (throwException) {

            } else {
                list.add("场景未设置");
            }
        }
        ParamMappingVo roundCountMappingVo = serviceConfigVo.getRoundCount();
        if (versionVo.getNeedRoundCount()) {
            if (roundCountMappingVo == null) {
                if (throwException) {

                } else {
                    list.add("分批数量未设置");
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {

                        } else {
                            list.add("分批数量未设置");
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {

                        } else {
                            list.add("未引用表单，不能映射表单属性");
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {

                        } else {
                            list.add("分批数量未设置");
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {

                            } else {
                                list.add("分批数量设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                            }
                        } else if (!Objects.equals(formAttributeVo.getHandler(), FormHandler.FORMNUMBER.getHandler())) {
                            if (throwException) {

                            } else {

                            }
                        }
                    }
                }
            }
        }
        ParamMappingVo protocolMappingVo = serviceConfigVo.getProtocol();
        if (versionVo.getNeedProtocol()) {
            if (protocolMappingVo == null) {
                if (throwException) {

                } else {
                    list.add("连接协议未设置");
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {

                        } else {
                            list.add("连接协议未设置");
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {

                        } else {
                            list.add("未引用表单，不能映射表单属性");
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {

                        } else {
                            list.add("连接协议未设置");
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {

                            } else {
                                list.add("连接协议设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                            }
                        } else if (!Objects.equals(formAttributeVo.getHandler(), neatlogic.framework.cmdb.enums.FormHandler.FORMPROTOCOL.getHandler())) {
                            if (throwException) {

                            } else {

                            }
                        }
                    }
                }
            }
        }
        ParamMappingVo executeUserMappingVo = serviceConfigVo.getExecuteUser();
        if (versionVo.getNeedExecuteUser()) {
            if (executeUserMappingVo == null) {
                if (throwException) {

                } else {
                    list.add("执行用户未设置");
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {

                        } else {
                            list.add("执行用户未设置");
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {

                        } else {
                            list.add("未引用表单，不能映射表单属性");
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {

                        } else {
                            list.add("执行用户未设置");
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {

                            } else {
                                list.add("执行用户设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                            }
                        } else if (!Objects.equals(formAttributeVo.getHandler(), FormHandler.FORMNUMBER.getHandler())) {
                            if (throwException) {

                            } else {

                            }
                        }
                    }
                }
            }
        }
        ParamMappingVo executeNodeParamMappingVo = serviceConfigVo.getExecuteNodeConfig();
        if (versionVo.getNeedExecuteNode()) {
            if (executeNodeParamMappingVo == null) {
                if (throwException) {

                } else {
                    list.add("执行目标未设置");
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {

                        } else {
                            list.add("执行目标未设置");
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {

                        } else {
                            list.add("未引用表单，不能映射表单属性");
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {

                        } else {
                            list.add("执行目标未设置");
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {

                            } else {
                                list.add("执行目标设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                            }
                        } else if (!Objects.equals(formAttributeVo.getHandler(), neatlogic.framework.cmdb.enums.FormHandler.FORMRESOURECES.getHandler())) {
                            if (throwException) {

                            } else {

                            }
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
                    if (throwException) {

                    } else {
                        list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“不存在");
                    }
                } else if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
                    if (throwException) {

                    } else {
                        list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“类型发生变化，由“" + runtimeParamMapping.getType() + "”变成“" + runtimeParamVo.getType() + "”");
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {

                        } else {
                            list.add("作业参数”" + name + "(" + key + ")“未设置");
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {

                        } else {
                            list.add("未引用表单，不能映射表单属性");
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {

                        } else {
                            list.add("作业参数”" + name + "(" + key + ")“未设置");
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {

                            } else {
                                list.add("作业参数”" + name + "(" + key + ")“设置错误，表单”" + formName + "“中没有找不到“" + value + "”属性");
                            }
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.IS_EMPTY.getValue())) {

                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.NOT_SET_UP.getValue())) {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (throwException) {

                        } else {
                            list.add("已引用表单，作业参数”" + name + "(" + key + ")“不能不设置");
                        }
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
                if (throwException) {

                } else {
                    list.add("作业参数“" + runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")“未映射");
                }
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            return String.join("；", list);
        }
        return null;
    }
}
