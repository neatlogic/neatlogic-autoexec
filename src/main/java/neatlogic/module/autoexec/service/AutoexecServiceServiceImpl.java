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
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.dto.FormVo;
import neatlogic.framework.form.exception.FormActiveVersionNotFoundExcepiton;
import neatlogic.framework.form.exception.FormAttributeNotFoundException;
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
        Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
        String formName = "";
        String formUuid = serviceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            FormVo formVo = formMapper.getFormByUuid(formUuid);
            if (formVo == null) {
                if (throwException) {
                    throw new FormNotFoundException(formUuid);
                } else {
                    return I18nUtils.getMessage("exception.framework.formnotfoundexception", formUuid);
                }
            }
            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            if (formVersionVo == null) {
                if (throwException) {
                    throw new FormActiveVersionNotFoundExcepiton(formVo.getName());
                } else {
                    return I18nUtils.getMessage("exception.framework.formactiveversionnotfoundexcepiton", formVo.getName());
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
                return I18nUtils.getMessage("exception.autoexec.autoexeccombopnotfoundexception", serviceVo.getCombopId());
            }
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(serviceVo.getCombopId());
        if (versionVo == null) {
            if (throwException) {
                throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
            } else {
                return I18nUtils.getMessage("exception.autoexec.autoexeccombopactiveversionnotfoundexception", autoexecCombopVo.getName());
            }
        }
        autoexecCombopService.needExecuteConfig(versionVo);
        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
        List<String> list = new ArrayList<>();
        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
        Long scenarioId = serviceConfigVo.getScenarioId();
        if (CollectionUtils.isNotEmpty(versionConfigVo.getScenarioList()) && scenarioId == null) {
            if (throwException) {
                throw new AutoexecScenarioIsRequiredException();
            } else {
                list.add(I18nUtils.getMessage("exception.autoexec.autoexecscenarioisrequiredexception"));
            }
        }
        ParamMappingVo roundCountMappingVo = serviceConfigVo.getRoundCount();
        if (versionVo.getNeedRoundCount()) {
            if (roundCountMappingVo == null) {
                if (throwException) {
                    throw new AutoexecRoundCountIsRequiredException();
                } else {
                    list.add(I18nUtils.getMessage("exception.autoexec.autoexecroundcountisrequiredexception"));
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecRoundCountIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecroundcountisrequiredexception"));
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecRoundCountIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecroundcountisrequiredexception"));
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                list.add(I18nUtils.getMessage("exception.framework.formattributenotfoundexception.1", formName, value));
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
                    throw new AutoexecProtocolIsRequiredException();
                } else {
                    list.add(I18nUtils.getMessage("exception.autoexec.autoexecprotocolisrequiredexception"));
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecProtocolIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecprotocolisrequiredexception"));
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecProtocolIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecprotocolisrequiredexception"));
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                list.add(I18nUtils.getMessage("exception.framework.formattributenotfoundexception.1", formName, value));
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
                    throw new AutoexecExecuteUserIsRequiredException();
                } else {
                    list.add(I18nUtils.getMessage("exception.autoexec.autoexecexecuteuserisrequiredexception"));
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecExecuteUserIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecexecuteuserisrequiredexception"));
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecExecuteUserIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecexecuteuserisrequiredexception"));
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                list.add(I18nUtils.getMessage("exception.framework.formattributenotfoundexception.1", formName, value));
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
                    throw new AutoexecExecuteNodeIsRequiredException();
                } else {
                    list.add(I18nUtils.getMessage("exception.autoexec.autoexecexecutenodeisrequiredexception"));
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecExecuteNodeIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecexecutenodeisrequiredexception"));
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecExecuteNodeIsRequiredException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecexecutenodeisrequiredexception"));
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                list.add(I18nUtils.getMessage("exception.framework.formattributenotfoundexception.1", formName, value));
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
                if (runtimeParamVo != null) {
                    name = runtimeParamVo.getName();
                }
                if (name == null) {
                    name = "";
                }
                if (runtimeParamVo == null) {
                    if (throwException) {
                        throw new AutoexecJobParamNotFoundException(autoexecCombopVo.getName(), name + "(" + key + ")");
                    } else {
                        list.add(I18nUtils.getMessage("exception.autoexec.autoexecjobparamnotfoundexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                    }
                } else if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
                    if (throwException) {
                        throw new AutoexecJobParamTypeChangedException(autoexecCombopVo.getName(), name + "(" + key + ")", runtimeParamMapping.getType(), runtimeParamVo.getType());
                    } else {
                        list.add(I18nUtils.getMessage("exception.autoexec.autoexecjobparamtypechangedexception", autoexecCombopVo.getName(), name + "(" + key + ")", runtimeParamMapping.getType(), runtimeParamVo.getType()));
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                list.add(I18nUtils.getMessage("exception.framework.formattributenotfoundexception.1", formName, value));
                            }
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.IS_EMPTY.getValue())) {

                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.NOT_SET_UP.getValue())) {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            list.add(I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                        }
                    }
                }
            }
        }
        if (MapUtils.isNotEmpty(runtimeParamMap)) {
            for (Map.Entry<String, AutoexecParamVo> entry : runtimeParamMap.entrySet()) {
                AutoexecParamVo runtimeParamVo = entry.getValue();
                if (throwException) {
                    throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")");
                } else {
                    list.add(I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")"));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            return String.join("；", list);
        }
        return null;
    }
}
