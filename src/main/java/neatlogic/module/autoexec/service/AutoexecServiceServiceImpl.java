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

package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.CombopNodeSpecify;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ServiceParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.form.attribute.core.FormAttributeDataConversionHandlerFactory;
import neatlogic.framework.form.attribute.core.IFormAttributeDataConversionHandler;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.dto.FormVo;
import neatlogic.framework.form.exception.FormActiveVersionNotFoundExcepiton;
import neatlogic.framework.form.exception.FormAttributeNotFoundException;
import neatlogic.framework.form.exception.FormAttributeRequiredException;
import neatlogic.framework.form.exception.FormNotFoundException;
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.process.dto.AutoexecJobBuilder;
import neatlogic.module.autoexec.process.util.CreateJobConfigUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                    jsonObj.put("description", $.t("表单：“{0}”不存在", formUuid));
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
                    jsonObj.put("description", $.t("表单：“{0}”没有激活版本", formVo.getName()));
                    reasonList.add(jsonObj);
                    return reasonList;
                }
            }
            String mainSceneUuid = formVersionVo.getFormConfig().getString("uuid");
            formVersionVo.setSceneUuid(mainSceneUuid);
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
                jsonObj.put("description", $.t("组合工具：“{0}”不存在", serviceVo.getCombopId()));
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
                jsonObj.put("description", $.t("组合工具：“{0}”没有激活版本", autoexecCombopVo.getName()));
                reasonList.add(jsonObj);
                return reasonList;
            }
        }
        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
        serviceConfigVo = mergeConfig(serviceConfigVo, versionVo);
        Long scenarioId = serviceConfigVo.getScenarioId();
        if (CollectionUtils.isNotEmpty(versionConfigVo.getScenarioList()) && scenarioId == null) {
            if (throwException) {
                throw new AutoexecScenarioIsRequiredException();
            } else {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("key", "scenarioId");
                jsonObj.put("description", $.t("场景必须设置"));
                reasonList.add(jsonObj);
            }
        }
        if (versionVo.getNeedRoundCount()) {
            ParamMappingVo roundCountMappingVo = serviceConfigVo.getRoundCount();
            if (roundCountMappingVo == null) {
                if (throwException) {
                    throw new AutoexecRoundCountIsRequiredException();
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "roundCount");
                    jsonObj.put("description", $.t("分批数量必须设置"));
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
                            jsonObj.put("description", $.t("分批数量必须设置"));
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
                            jsonObj.put("description", $.t("服务目录未引用表单，不能映射表单属性"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecRoundCountIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "roundCount");
                            jsonObj.put("description", $.t("分批数量必须设置"));
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
                                jsonObj.put("description", $.t("表单”{0}“中找不到“{1}”属性", formName, value));
                                reasonList.add(jsonObj);
                            }
                        }
                    }
                }
            }
        }
        if (versionVo.getNeedProtocol()) {
            ParamMappingVo protocolMappingVo = serviceConfigVo.getProtocol();
            if (protocolMappingVo == null) {
                if (throwException) {
                    throw new AutoexecProtocolIsRequiredException();
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "protocol");
                    jsonObj.put("description", $.t("连接协议必须设置"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = protocolMappingVo.getValue();
                String mappingMode = protocolMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecProtocolIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "protocol");
                            jsonObj.put("description", $.t("连接协议必须设置"));
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
                            jsonObj.put("description", $.t("服务目录未引用表单，不能映射表单属性"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecProtocolIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "protocol");
                            jsonObj.put("description", $.t("连接协议必须设置"));
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
                                jsonObj.put("description", $.t("表单”{0}“中找不到“{1}”属性", formName, value));
                                reasonList.add(jsonObj);
                            }
                        }
                    }
                }
            }
        }
        if (versionVo.getNeedExecuteUser()) {
            ParamMappingVo executeUserMappingVo = serviceConfigVo.getExecuteUser();
            if (executeUserMappingVo == null) {
                if (throwException) {
                    throw new AutoexecExecuteUserIsRequiredException();
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "executeUser");
                    jsonObj.put("description", $.t("执行用户必须设置"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = executeUserMappingVo.getValue();
                String mappingMode = executeUserMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecExecuteUserIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeUser");
                            jsonObj.put("description", $.t("执行用户必须设置"));
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
                            jsonObj.put("description", $.t("服务目录未引用表单，不能映射表单属性"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecExecuteUserIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeUser");
                            jsonObj.put("description", $.t("执行用户必须设置"));
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
                                jsonObj.put("description", $.t("表单”{0}“中找不到“{1}”属性", formName, value));
                                reasonList.add(jsonObj);
                            }
                        }
                    }
                }
            }
        }
        if (versionVo.getNeedExecuteNode()) {
            ParamMappingVo executeNodeParamMappingVo = serviceConfigVo.getExecuteNodeConfig();
            if (executeNodeParamMappingVo == null) {
                if (throwException) {
                    throw new AutoexecExecuteNodeIsRequiredException();
                } else {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("key", "executeNodeConfig");
                    jsonObj.put("description", $.t("执行目标必须设置"));
                    reasonList.add(jsonObj);
                }
            } else {
                Object value = executeNodeParamMappingVo.getValue();
                String mappingMode = executeNodeParamMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecExecuteNodeIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeNodeConfig");
                            jsonObj.put("description", $.t("执行目标必须设置"));
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
                            jsonObj.put("description", $.t("服务目录未引用表单，不能映射表单属性"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecExecuteNodeIsRequiredException();
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", "executeNodeConfig");
                            jsonObj.put("description", $.t("执行目标必须设置"));
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
                                jsonObj.put("description", $.t("表单”{0}“中找不到“{1}”属性", formName, value));
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
            runtimeParamMap = runtimeParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getKey, e -> e));
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
                        jsonObj.put("description", $.t("组合工具“{0}”的作业参数中没有“{1}”的参数", autoexecCombopVo.getName(), name + "(" + key + ")"));
                        reasonList.add(jsonObj);
                    }
                } else if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
                    if (throwException) {
                        throw new AutoexecJobParamTypeChangedException(autoexecCombopVo.getName(), name + "(" + key + ")", runtimeParamMapping.getType(), runtimeParamVo.getType());
                    } else {
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("key", key);
                        jsonObj.put("description", $.t("组合工具“{0}”的作业参数“{1}“类型发生变化，由“{2}”变成“{3}”类型", autoexecCombopVo.getName(), name + "(" + key + ")", runtimeParamMapping.getType(), runtimeParamVo.getType()));
                        reasonList.add(jsonObj);
                    }
                } else if (Objects.equals(mappingMode, ServiceParamMappingMode.CONSTANT.getValue())) {
                    if (value == null) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", $.t("组合工具“{0}”的作业参数“{1}“必须设置", autoexecCombopVo.getName(), name + "(" + key + ")"));
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
                            jsonObj.put("description", $.t("服务目录未引用表单，不能映射表单属性"));
                            reasonList.add(jsonObj);
                        }
                    } else if (StringUtils.isBlank((String) value)) {
                        if (throwException) {
                            throw new AutoexecJobParamIsRequiredException(autoexecCombopVo.getName(), name + "(" + key + ")");
                        } else {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("key", key);
                            jsonObj.put("description", $.t("组合工具“{0}”的作业参数“{1}“必须设置", autoexecCombopVo.getName(), name + "(" + key + ")"));
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
                                jsonObj.put("description", $.t("表单”{0}“中找不到“{1}”属性", formName, value));
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
                            jsonObj.put("description", $.t("组合工具“{0}”的作业参数“{1}“不能为空", autoexecCombopVo.getName(), name + "(" + key + ")"));
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
                            jsonObj.put("description", $.t("组合工具“{0}”的作业参数“{1}“必须设置", autoexecCombopVo.getName(), name + "(" + key + ")"));
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
                    jsonObj.put("description", $.t("组合工具“{0}”的作业参数“{1}“必须设置", autoexecCombopVo.getName(), runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")"));
                    reasonList.add(jsonObj);
                }
            }
        }
        return reasonList;
    }

    @Override
    public AutoexecJobBuilder getAutoexecJobBuilder(
            AutoexecServiceVo autoexecServiceVo,
            AutoexecCombopVersionVo autoexecCombopVersionVo,
            String name,
            Long scenarioId,
            JSONArray formAttributeDataList,
            JSONArray hidecomponentList,
            Integer roundCount,
            String executeUser,
            Long protocol,
            AutoexecCombopExecuteNodeConfigVo executeNodeConfig,
            JSONObject runtimeParamMap,
            ParamMappingVo runnerGroup,
            ParamMappingVo runnerGroupTag
    ) {
        AutoexecServiceConfigVo config = autoexecServiceVo.getConfig();
        config = mergeConfig(config, autoexecCombopVersionVo);
        AutoexecJobBuilder builder = new AutoexecJobBuilder(autoexecCombopVersionVo.getCombopId());
        builder.setJobName(name);
        if (scenarioId == null) {
            scenarioId = config.getScenarioId();
        }
        if (scenarioId != null) {
            builder.setScenarioId(scenarioId);
        }

        // 执行器组
        if (runnerGroup != null) {
            builder.setRunnerGroup(runnerGroup);
        } else {
            if (config.getRunnerGroup() != null) {
                builder.setRunnerGroup(config.getRunnerGroup());
            }
        }

        // 执行器组标签
        if (runnerGroupTag != null) {
            builder.setRunnerGroupTag(runnerGroupTag);
        } else {
            if (config.getRunnerGroupTag() != null) {
                builder.setRunnerGroupTag(config.getRunnerGroupTag());
            }
        }
        Map<String, Object> formAttributeDataMap = new HashMap<>();
        String formUuid = autoexecServiceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            if (CollectionUtils.isEmpty(formAttributeDataList)) {
                throw new ParamNotExistsException("formAttributeDataList");
            }

            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            String mainSceneUuid = formVersionVo.getFormConfig().getString("uuid");
            formVersionVo.setSceneUuid(mainSceneUuid);
            List<FormAttributeVo> formAttributeVoList = formVersionVo.getFormAttributeList();
            Map<String, FormAttributeVo> formAttributeVoMap = formAttributeVoList.stream().collect(Collectors.toMap(FormAttributeVo::getUuid, e -> e));
            if (CollectionUtils.isEmpty(hidecomponentList)) {
                hidecomponentList = new JSONArray();
            }
            for (int i = 0; i < formAttributeDataList.size(); i++) {
                JSONObject formAttributeData = formAttributeDataList.getJSONObject(i);
                if (formAttributeData == null) {
                    continue;
                }
                String attributeUuid = formAttributeData.getString("attributeUuid");
                if (StringUtils.isBlank(attributeUuid)) {
                    continue;
                }
                Object dataList = formAttributeData.get("dataList");
                if (dataList == null) {
                    continue;
                }
                FormAttributeVo formAttributeVo = formAttributeVoMap.get(attributeUuid);
                if (formAttributeVo == null) {
                    continue;
                }
                IFormAttributeDataConversionHandler handler = FormAttributeDataConversionHandlerFactory.getHandler(formAttributeVo.getHandler());
                if (handler != null) {
                    Object simpleValue = handler.getSimpleValue(dataList);
                    formAttributeDataMap.put(attributeUuid, simpleValue);
                } else {
                    formAttributeDataMap.put(attributeUuid, dataList);
                }
            }
            for (FormAttributeVo formAttributeVo : formAttributeVoList) {
                if (formAttributeVo.isRequired()) {
                    if (hidecomponentList.contains(formAttributeVo.getUuid())) {
                        continue;
                    }
                    if (formAttributeDataMap.containsKey(formAttributeVo.getUuid())) {
                        continue;
                    }
                    throw new FormAttributeRequiredException(formAttributeVo.getLabel());
                }
            }
        }

        // 如果服务编辑页设置了表单，且分批数量、执行目标、连接协议、执行账号、作业参数是必填时，要么映射表单组件，要么映射常量（必填）。
        // 如果服务编辑页没有设置了表单，那么分批数量、执行目标、连接协议、执行账号、作业参数等可填也可不填，不填的话，在服务创建作业时再填。
        if (autoexecCombopVersionVo.getNeedRoundCount()) {
            ParamMappingVo roundCountParamMappingVo = config.getRoundCount();
            if (roundCountParamMappingVo != null) {
                if (Objects.equals(roundCountParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue()) && roundCountParamMappingVo.getValue() != null) {
                    builder.setRoundCount((Integer) roundCountParamMappingVo.getValue());
                } else {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (Objects.equals(roundCountParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                            Object value = formAttributeDataMap.get(roundCountParamMappingVo.getValue().toString());
                            if (value != null) {
                                builder.setRoundCount((Integer) value);
                            }
                        }
                    } else {
                        if (roundCount != null) {
                            builder.setRoundCount(roundCount);
                        } else {
                            throw new ParamNotExistsException("分批数量(roundCount)必须设置， 请联系管理员重新编辑该服务");
                        }
                    }
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = new AutoexecCombopExecuteConfigVo();
        if (autoexecCombopVersionVo.getNeedExecuteUser()) {
            ParamMappingVo executeUserParamMappingVo = config.getExecuteUser();
            if (executeUserParamMappingVo != null) {
                if (Objects.equals(executeUserParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue())) {
                    executeConfigVo.setExecuteUser(executeUserParamMappingVo);
                } else {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (Objects.equals(executeUserParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue()) && executeUserParamMappingVo.getValue() != null) {
                            Object value = formAttributeDataMap.get(executeUserParamMappingVo.getValue().toString());
                            if (value != null) {
                                ParamMappingVo paramMappingVo = new ParamMappingVo();
                                paramMappingVo.setMappingMode(ServiceParamMappingMode.CONSTANT.getValue());
                                paramMappingVo.setValue(value);
                                executeConfigVo.setExecuteUser(paramMappingVo);
                            }
                        }
                    } else {
                        if (executeUser != null) {
                            ParamMappingVo paramMappingVo = new ParamMappingVo();
                            paramMappingVo.setMappingMode(ServiceParamMappingMode.CONSTANT.getValue());
                            paramMappingVo.setValue(executeUser);
                            executeConfigVo.setExecuteUser(paramMappingVo);
                        } else {
                            throw new ParamNotExistsException("执行用户(executeUser)必须设置， 请联系管理员重新编辑该服务");
                        }
                    }
                }
            }
        }
        if (autoexecCombopVersionVo.getNeedProtocol()) {
            ParamMappingVo protocolParamMappingVo = config.getProtocol();
            if (protocolParamMappingVo != null) {
                if (Objects.equals(protocolParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue()) && protocolParamMappingVo.getValue() != null) {
                    executeConfigVo.setProtocolId((Long) protocolParamMappingVo.getValue());
                } else {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (Objects.equals(protocolParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                            Object value = formAttributeDataMap.get(protocolParamMappingVo.getValue().toString());
                            if (value != null) {
                                executeConfigVo.setProtocolId((Long) value);
                            }
                        }
                    } else {
                        if (protocol != null) {
                            executeConfigVo.setProtocolId(protocol);
                        } else {
                            throw new ParamNotExistsException("连接协议(protocol)必须设置， 请联系管理员重新编辑该服务");
                        }
                    }
                }
            }
        }
        if (autoexecCombopVersionVo.getNeedExecuteNode()) {
            ParamMappingVo executeNodeParamMappingVo = config.getExecuteNodeConfig();
            if (executeNodeParamMappingVo != null) {
                if (Objects.equals(executeNodeParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue()) && executeNodeParamMappingVo.getValue() != null) {
                    Object value = executeNodeParamMappingVo.getValue();
                    if (value instanceof AutoexecCombopExecuteNodeConfigVo) {
                        executeConfigVo.setExecuteNodeConfig((AutoexecCombopExecuteNodeConfigVo) value);
                    } else if (value instanceof JSONObject) {
                        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject((JSONObject) executeNodeParamMappingVo.getValue(), AutoexecCombopExecuteNodeConfigVo.class);
                        executeConfigVo.setExecuteNodeConfig(executeNodeConfigVo);
                    }
                } else {
                    if (StringUtils.isNotBlank(formUuid)) {
                        if (Objects.equals(executeNodeParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                            Object value = formAttributeDataMap.get(executeNodeParamMappingVo.getValue().toString());
                            if (value != null) {
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.add(value);
                                AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = CreateJobConfigUtil.getExecuteNodeConfig(jsonArray);
                                executeConfigVo.setExecuteNodeConfig(executeNodeConfigVo);
                            }
                        }
                    } else {
                        if (executeNodeConfig.isNull()) {
                            throw new ParamNotExistsException("执行目标(executeNodeConfig)必须设置， 请联系管理员重新编辑该服务");
                        } else {
                            executeConfigVo.setExecuteNodeConfig(executeNodeConfig);
                        }
                    }
                }
            }
        }
        builder.setExecuteConfig(executeConfigVo);
        AutoexecCombopVersionConfigVo versionConfigVo = autoexecCombopVersionVo.getConfig();
        List<AutoexecParamVo> lastRuntimeParamList = versionConfigVo.getRuntimeParamList();
        JSONObject param = new JSONObject();
        if (CollectionUtils.isNotEmpty(lastRuntimeParamList)) {
            List<ParamMappingVo> runtimeParamList = config.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                for (ParamMappingVo paramMappingVo : runtimeParamList) {
                    if (paramMappingVo == null) {
                        continue;
                    }
                    String key = paramMappingVo.getKey();
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    Object value = paramMappingVo.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (Objects.equals(paramMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue())) {
                        param.put(key, value);
                    } else {
                        if (StringUtils.isNotBlank(formUuid)) {
                            if (Objects.equals(paramMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                                Object formAttrValue = formAttributeDataMap.get(value.toString());
                                if (formAttrValue != null) {
                                    param.put(key, formAttrValue);
                                }
                            }
                        }
                    }
                }
            }
            if (MapUtils.isNotEmpty(runtimeParamMap)) {
                param.putAll(runtimeParamMap);
            }
            for (AutoexecParamVo autoexecParamVo : lastRuntimeParamList) {
                if (param.containsKey(autoexecParamVo.getKey())) {
                    continue;
                }
                if (!Objects.equals(autoexecParamVo.getIsRequired(), 1)) {
                    continue;
                }
                if(autoexecParamVo.getDefaultValue() != null) {
                    continue;
                }
                throw new AutoexecJobParamNotExistException(autoexecParamVo.getName(), autoexecParamVo.getKey());
            }
        }
        builder.setParam(param);
        return builder;
    }

    private AutoexecServiceConfigVo mergeConfig(AutoexecServiceConfigVo serviceConfig, AutoexecCombopVersionVo autoexecCombopVersionVo) {
        AutoexecServiceConfigVo config = JSONObject.parseObject(JSONObject.toJSONString(serviceConfig), AutoexecServiceConfigVo.class);
        autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo);
        AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
        AutoexecCombopExecuteConfigVo executeConfig = versionConfig.getExecuteConfig();
        if (executeConfig != null) {
            if (autoexecCombopVersionVo.getNeedRoundCount()) {
                if (executeConfig.getRoundCount() != null) {
                    ParamMappingVo roundCount = new ParamMappingVo();
                    roundCount.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    roundCount.setValue(executeConfig.getRoundCount());
                    config.setRoundCount(roundCount);
                }
            } else {
                config.setRoundCount(null);
            }
            if (autoexecCombopVersionVo.getNeedExecuteNode()) {
                if (Objects.equals(executeConfig.getWhenToSpecify(), CombopNodeSpecify.NOW.getValue()) && executeConfig.getExecuteNodeConfig() != null) {
                    ParamMappingVo executeNodeConfig = new ParamMappingVo();
                    executeNodeConfig.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    executeNodeConfig.setValue(executeConfig.getExecuteNodeConfig());
                    config.setExecuteNodeConfig(executeNodeConfig);
                }
            } else {
                config.setExecuteNodeConfig(null);
            }
            if (autoexecCombopVersionVo.getNeedProtocol()) {
                if (executeConfig.getProtocolId() != null && config.getProtocol() == null) {
                    ParamMappingVo protocol = new ParamMappingVo();
                    protocol.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    protocol.setValue(executeConfig.getProtocolId());
                    config.setProtocol(protocol);
                }
            } else {
                config.setProtocol(null);
            }
            if (autoexecCombopVersionVo.getNeedExecuteUser()) {
                ParamMappingVo executeUser = executeConfig.getExecuteUser();
                if (executeUser != null) {
                    if (Objects.equals(executeUser.getMappingMode(), ParamMappingMode.CONSTANT.getValue())) {
                        if (config.getExecuteUser() == null) {
                            config.setExecuteUser(executeUser);
                        }
                    } else if (Objects.equals(executeUser.getMappingMode(), ParamMappingMode.RUNTIME_PARAM.getValue())) {
                        config.setExecuteUser(executeUser);
                    }
                }
            } else {
                config.setExecuteUser(null);
            }
            ParamMappingVo runnerGroupTag = executeConfig.getRunnerGroupTag();
            if (runnerGroupTag != null) {
                if (Objects.equals(runnerGroupTag.getMappingMode(), ParamMappingMode.CONSTANT.getValue())) {
                    if (config.getRunnerGroupTag() == null) {
                        config.setRunnerGroupTag(runnerGroupTag);
                    }
                } else if (Objects.equals(runnerGroupTag.getMappingMode(), ParamMappingMode.RUNTIME_PARAM.getValue())) {
                    config.setRunnerGroupTag(runnerGroupTag);
                }
            }
            ParamMappingVo runnerGroup = executeConfig.getRunnerGroup();
            if (runnerGroup != null) {
                if (Objects.equals(runnerGroup.getMappingMode(), ParamMappingMode.CONSTANT.getValue())) {
                    if (config.getRunnerGroup() == null) {
                        config.setRunnerGroup(runnerGroup);
                    }
                } else if (Objects.equals(runnerGroup.getMappingMode(), ParamMappingMode.RUNTIME_PARAM.getValue())) {
                    config.setRunnerGroup(runnerGroup);
                }
            }
        }

        if (CollectionUtils.isEmpty(versionConfig.getScenarioList())) {
            config.setScenarioId(null);
        }
        return config;
    }
}
