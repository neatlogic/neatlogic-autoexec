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
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
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
                jsonObj.put("description", $.t("场景必须设置"));
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
        ParamMappingVo protocolMappingVo = serviceConfigVo.getProtocol();
        if (versionVo.getNeedProtocol()) {
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
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
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
        ParamMappingVo executeUserMappingVo = serviceConfigVo.getExecuteUser();
        if (versionVo.getNeedExecuteUser()) {
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
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
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
        ParamMappingVo executeNodeParamMappingVo = serviceConfigVo.getExecuteNodeConfig();
        if (versionVo.getNeedExecuteNode()) {
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
                Object value = roundCountMappingVo.getValue();
                String mappingMode = roundCountMappingVo.getMappingMode();
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
}
