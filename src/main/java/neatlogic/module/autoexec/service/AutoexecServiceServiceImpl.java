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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
    public JSONArray checkConfigExpired(AutoexecServiceVo serviceVo, boolean throwException) {
        JSONArray reasonList = new JSONArray();
        Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
        String formName = "";
        String formUuid = serviceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            FormVo formVo = formMapper.getFormByUuid(formUuid);
            if (formVo == null) {
                if (throwException) {
                    throw new FormNotFoundException(formUuid);
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "formUuid");
                    jsonObj.put("description", I18nUtils.getMessage("exception.framework.formnotfoundexception", formUuid));
                    reasonList.add(jsonObj);
                    return reasonList;
                }
            }
            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            if (formVersionVo == null) {
                if (throwException) {
                    throw new FormActiveVersionNotFoundExcepiton(formVo.getName());
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "formUuid");
                    jsonObj.put("description", I18nUtils.getMessage("exception.framework.formactiveversionnotfoundexcepiton", formVo.getName()));
                    reasonList.add(jsonObj);
                    return reasonList;
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
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("key", "combopId");
                jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexeccombopnotfoundexception", serviceVo.getCombopId()));
                reasonList.add(jsonObj);
                return reasonList;
            }
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(serviceVo.getCombopId());
        if (versionVo == null) {
            if (throwException) {
                throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
            } else {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("key", "combopId");
                jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexeccombopactiveversionnotfoundexception", autoexecCombopVo.getName()));
                reasonList.add(jsonObj);
                return reasonList;
            }
        }
        autoexecCombopService.needExecuteConfig(versionVo);
        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
//        List<String> list = new ArrayList<>();
        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
        Long scenarioId = serviceConfigVo.getScenarioId();
        if (CollectionUtils.isNotEmpty(versionConfigVo.getScenarioList()) && scenarioId == null) {
            if (throwException) {
                throw new AutoexecScenarioIsRequiredException();
            } else {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("key", "scenarioId");
                jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecscenarioisrequiredexception"));
                reasonList.add(jsonObj);
            }
        }
        ParamMappingVo roundCountMappingVo = serviceConfigVo.getRoundCount();
        if (versionVo.getNeedRoundCount()) {
            if (roundCountMappingVo == null) {
                if (throwException) {
                    throw new AutoexecRoundCountIsRequiredException();
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "roundCount");
                    jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecroundcountisrequiredexception"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecRoundCountIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "roundCount");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecroundcountisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "roundCount");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecRoundCountIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "roundCount");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecroundcountisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("key", "roundCount");
                                jsonObj.put("description", I18nUtils.getMessage("exception.framework.formattributenotfoundexception.b", formName, value));
                                reasonList.add(jsonObj);
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
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "protocol");
                    jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecprotocolisrequiredexception"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecProtocolIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "protocol");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecprotocolisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "protocol");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecProtocolIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "protocol");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecprotocolisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("key", "protocol");
                                jsonObj.put("description", I18nUtils.getMessage("exception.framework.formattributenotfoundexception.b", formName, value));
                                reasonList.add(jsonObj);
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
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "executeUser");
                    jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecexecuteuserisrequiredexception"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecExecuteUserIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeUser");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecexecuteuserisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeUser");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecExecuteUserIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeUser");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecexecuteuserisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("key", "executeUser");
                                jsonObj.put("description", I18nUtils.getMessage("exception.framework.formattributenotfoundexception.b", formName, value));
                                reasonList.add(jsonObj);
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
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "executeNodeConfig");
                    jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecexecutenodeisrequiredexception"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecExecuteNodeIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeNodeConfig");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecexecutenodeisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeNodeConfig");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecExecuteNodeIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeNodeConfig");
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecexecutenodeisrequiredexception"));
                            reasonList.add(jsonObj);
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("key", "executeNodeConfig");
                                jsonObj.put("description", I18nUtils.getMessage("exception.framework.formattributenotfoundexception.b", formName, value));
                                reasonList.add(jsonObj);
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
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("key", key);
                        jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamnotfoundexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                        reasonList.add(jsonObj);
                    }
                } else if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
                    if (throwException) {
                        throw new AutoexecJobParamTypeChangedException(autoexecCombopVo.getName(), name + "(" + key + ")", runtimeParamMapping.getType(), runtimeParamVo.getType());
                    } else {
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("key", key);
                        jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamtypechangedexception", autoexecCombopVo.getName(), name + "(" + key + ")", runtimeParamMapping.getType(), runtimeParamVo.getType()));
                        reasonList.add(jsonObj);
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                            reasonList.add(jsonObj);
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.FORMATTR.getValue())) {
                    if (StringUtils.isBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecServiceNotReferencedFormException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecservicenotreferencedformexception"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                            reasonList.add(jsonObj);
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get((String) value);
                        if (formAttributeVo == null) {
                            if (throwException) {
                                throw new FormAttributeNotFoundException(formName, (String) value);
                            } else {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("key", key);
                                jsonObj.put("description", I18nUtils.getMessage("exception.framework.formattributenotfoundexception.b", formName, value));
                                reasonList.add(jsonObj);
                            }
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.IS_EMPTY.getValue())) {
                    if (Objects.equals(runtimeParamVo.getIsRequired(), 1)) {
                        if (throwException) {
                            throw new AutoexecJobParamCannotBeEmptyException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamcannotbeemptyexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                            reasonList.add(jsonObj);
                        }
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.NOT_SET_UP.getValue())) {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), name + "(" + key + ")"));
                            reasonList.add(jsonObj);
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
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", runtimeParamVo.getKey());
                    jsonObj.put("description", I18nUtils.getMessage("exception.autoexec.autoexecjobparamisrequiredexception", autoexecCombopVo.getName(), runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")"));
                    reasonList.add(jsonObj);
                }
            }
        }
        return reasonList;
    }
}
