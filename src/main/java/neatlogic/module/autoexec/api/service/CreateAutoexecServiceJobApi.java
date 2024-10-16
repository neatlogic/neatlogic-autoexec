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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.constvalue.ServiceParamMappingMode;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobParamNotExistException;
import neatlogic.framework.autoexec.exception.AutoexecServiceConfigExpiredException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.exception.FormAttributeRequiredException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.FormUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecServiceJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private FormMapper formMapper;

    @Override
    public String getName() {
        return "作业创建（来自服务）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "serviceId", type = ApiParamType.LONG, isRequired = true, desc = "服务ID"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名"),
            @Param(name = "formAttributeDataList", type = ApiParamType.JSONARRAY, desc = "表单属性数据列表"),
            @Param(name = "hidecomponentList", type = ApiParamType.JSONARRAY, desc = "隐藏表单属性列表"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景ID"),
            @Param(name = "roundCount", type = ApiParamType.INTEGER, desc = "分批数量"),
            @Param(name = "protocol", type = ApiParamType.LONG, desc = "协议ID"),
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "执行用户"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
            @Param(name = "runtimeParamMap", type = ApiParamType.JSONOBJECT, desc = "作业参数"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "触发方式")
    })
    @Output({
    })
    @Description(desc = "作业创建（来自服务）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long serviceId = paramObj.getLong("serviceId");
        AutoexecServiceVo autoexecServiceVo = autoexecServiceMapper.getAutoexecServiceById(serviceId);
        if (autoexecServiceVo == null) {
            throw new AutoexecServiceNotFoundException(serviceId);
        }
        if (Objects.equals(autoexecServiceVo.getConfigExpired(), 1)) {
            throw new AutoexecServiceConfigExpiredException(autoexecServiceVo.getName());
        }
        List<Long> upwardIdList = autoexecServiceMapper.getUpwardIdListByLftAndRht(autoexecServiceVo.getLft(), autoexecServiceVo.getRht());
        AutoexecServiceSearchVo searchVo = new AutoexecServiceSearchVo();
        searchVo.setServiceIdList(upwardIdList);
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        searchVo.setAuthenticationInfoVo(authenticationInfoVo);
        int count = autoexecServiceMapper.getAllVisibleCount(searchVo);
        if (count < upwardIdList.size()) {
            throw new PermissionDeniedException();
        }
        Long combopId = autoexecServiceVo.getCombopId();
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(combopId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(combopId);
        }
        AutoexecCombopVersionConfigVo versionConfigVo = autoexecCombopVersionVo.getConfig();
        List<AutoexecParamVo> lastRuntimeParamList = versionConfigVo.getRuntimeParamList();
        String triggerType = paramObj.getString("triggerType");
        Long planStartTime = paramObj.getLong("planStartTime");
        AutoexecJobVo autoexecJobVo = new AutoexecJobVo();
        autoexecJobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        autoexecJobVo.setOperationId(combopId);
        autoexecJobVo.setInvokeId(serviceId);
        autoexecJobVo.setRouteId(serviceId.toString());
        autoexecJobVo.setCombopId(combopId);
        autoexecJobVo.setName(paramObj.getString("name"));
        autoexecJobVo.setSource(JobSource.SERVICE.getValue());
        autoexecJobVo.setTriggerType(triggerType);
        if (planStartTime != null) {
            autoexecJobVo.setPlanStartTime(new Date(planStartTime));
        }
        AutoexecServiceConfigVo config = autoexecServiceVo.getConfig();
        Long scenarioId = paramObj.getLong("scenarioId");
        if (scenarioId == null) {
            scenarioId = config.getScenarioId();
        }
        if (scenarioId != null) {
            autoexecJobVo.setScenarioId(scenarioId);
        }

        autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo);
        boolean needExecuteUser = autoexecCombopVersionVo.getNeedExecuteUser();
        boolean needExecuteNode = autoexecCombopVersionVo.getNeedExecuteNode();
        boolean needProtocol = autoexecCombopVersionVo.getNeedProtocol();
        boolean needRoundCount = autoexecCombopVersionVo.getNeedRoundCount();
        // 如果服务编辑页设置了表单，且分批数量、执行目标、连接协议、执行账号、作业参数是必填时，要么映射表单组件，要么映射常量（必填）。
        // 如果服务编辑页没有设置了表单，那么分批数量、执行目标、连接协议、执行账号、作业参数等可填也可不填，不填的话，在服务创建作业时再填。
        String formUuid = autoexecServiceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            JSONArray formAttributeDataList = paramObj.getJSONArray("formAttributeDataList");
            JSONArray hidecomponentList = paramObj.getJSONArray("hidecomponentList");
            if (CollectionUtils.isEmpty(formAttributeDataList)) {
                throw new ParamNotExistsException("formAttributeDataList");
            }
            if (CollectionUtils.isEmpty(hidecomponentList)) {
                hidecomponentList = new JSONArray();
            }
            Map<String, Object> formAttributeDataMap = new HashMap<>();
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
                formAttributeDataMap.put(attributeUuid, dataList);
            }
            Map<String, String> attributeUuid2HandlerMap = new HashMap<>();
            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            String mainSceneUuid = formVersionVo.getFormConfig().getString("uuid");
            formVersionVo.setSceneUuid(mainSceneUuid);
            List<FormAttributeVo> formAttributeVoList = formVersionVo.getFormAttributeList();
            for (FormAttributeVo formAttributeVo : formAttributeVoList) {
                String uuid = formAttributeVo.getUuid();
                attributeUuid2HandlerMap.put(uuid, formAttributeVo.getHandler());
                if (formAttributeVo.isRequired()) {
                    if (hidecomponentList.contains(uuid)) {
                        continue;
                    }
                    if (formAttributeDataMap.containsKey(uuid)) {
                        continue;
                    }
                    throw new FormAttributeRequiredException(formAttributeVo.getLabel());
                }
            }
            List<String> formSelectAttributeList = new ArrayList<>();
            formSelectAttributeList.add(FormHandler.FORMSELECT.getHandler());
            formSelectAttributeList.add(FormHandler.FORMCHECKBOX.getHandler());
            formSelectAttributeList.add(FormHandler.FORMRADIO.getHandler());
            if (config != null) {
                ParamMappingVo roundCountParamMappingVo = config.getRoundCount();
                if (needRoundCount && roundCountParamMappingVo != null) {
                    if (Objects.equals(roundCountParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue())) {
                        autoexecJobVo.setRoundCount((Integer) roundCountParamMappingVo.getValue());
                    } else if (Objects.equals(roundCountParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                        Object value = formAttributeDataMap.get(roundCountParamMappingVo.getValue());
                        if (value != null) {
                            autoexecJobVo.setRoundCount((Integer) value);
                        }
                    }
                }
                AutoexecCombopExecuteConfigVo executeConfigVo = new AutoexecCombopExecuteConfigVo();
                ParamMappingVo executeUserParamMappingVo = config.getExecuteUser();
                if (needExecuteUser && executeUserParamMappingVo != null) {
                    if (Objects.equals(executeUserParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue())) {
                        executeConfigVo.setExecuteUser(executeUserParamMappingVo);
                    } else if (Objects.equals(executeUserParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                        Object value = formAttributeDataMap.get(executeUserParamMappingVo.getValue());
                        if (value != null) {
                            ParamMappingVo paramMappingVo = new ParamMappingVo();
                            paramMappingVo.setMappingMode(ServiceParamMappingMode.CONSTANT.getValue());
                            paramMappingVo.setValue(value);
                            executeConfigVo.setExecuteUser(paramMappingVo);
                        }
                    }
                }
                ParamMappingVo protocolParamMappingVo = config.getProtocol();
                if (needProtocol && protocolParamMappingVo != null) {
                    if (Objects.equals(protocolParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue())) {
                        executeConfigVo.setProtocolId((Long) protocolParamMappingVo.getValue());
                    } else if (Objects.equals(protocolParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                        Object value = formAttributeDataMap.get(protocolParamMappingVo.getValue());
                        if (value != null) {
                            executeConfigVo.setProtocolId((Long) value);
                        }
                    }
                }
                ParamMappingVo executeNodeParamMappingVo = config.getExecuteNodeConfig();
                if (needExecuteNode && executeNodeParamMappingVo != null) {
                    if (Objects.equals(executeNodeParamMappingVo.getMappingMode(), ServiceParamMappingMode.CONSTANT.getValue())) {
                        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject((JSONObject) executeNodeParamMappingVo.getValue(), AutoexecCombopExecuteNodeConfigVo.class);
                        executeConfigVo.setExecuteNodeConfig(executeNodeConfigVo);
                    } else if (Objects.equals(executeNodeParamMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                        Object value = formAttributeDataMap.get(executeNodeParamMappingVo.getValue());
                        if (value != null) {
                            AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject((JSONObject) value, AutoexecCombopExecuteNodeConfigVo.class);
                            executeConfigVo.setExecuteNodeConfig(executeNodeConfigVo);
                        }
                    }
                }
                autoexecJobVo.setExecuteConfig(executeConfigVo);
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
                            } else if (Objects.equals(paramMappingVo.getMappingMode(), ServiceParamMappingMode.FORMATTR.getValue())) {
                                Object formAttrValue = formAttributeDataMap.get(value);
                                if (formAttrValue != null) {
                                    if (formSelectAttributeList.contains(attributeUuid2HandlerMap.get(value))) {
                                        Object valueObject = FormUtil.getFormSelectAttributeValueByOriginalValue(formAttrValue);
                                        param.put(key, valueObject);
                                    } else {
                                        param.put(key, formAttrValue);
                                    }
                                }
                            }
                        }
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
                autoexecJobVo.setParam(param);
            }
        } else {
            if (config != null) {
                ParamMappingVo roundCountParamMappingVo = config.getRoundCount();
                if (needRoundCount && roundCountParamMappingVo != null) {
                    if (roundCountParamMappingVo.getValue() != null) {
                        autoexecJobVo.setRoundCount((Integer) roundCountParamMappingVo.getValue());
                    } else {
                        Integer roundCount = paramObj.getInteger("roundCount");
                        if (roundCount != null) {
                            autoexecJobVo.setRoundCount(roundCount);
                        } else {
                            throw new ParamNotExistsException("roundCount");
                        }
                    }
                }
                AutoexecCombopExecuteConfigVo executeConfigVo = new AutoexecCombopExecuteConfigVo();
                ParamMappingVo executeUserParamMappingVo = config.getExecuteUser();
                if (needExecuteUser && executeUserParamMappingVo != null) {
                    if (executeUserParamMappingVo.getValue() != null) {
                        executeConfigVo.setExecuteUser(executeUserParamMappingVo);
                    } else {
                        String executeUser = paramObj.getString("executeUser");
                        if (executeUser != null) {
                            ParamMappingVo paramMappingVo = new ParamMappingVo();
                            paramMappingVo.setMappingMode(ServiceParamMappingMode.CONSTANT.getValue());
                            paramMappingVo.setValue(executeUser);
                            executeConfigVo.setExecuteUser(paramMappingVo);
                        } else {
                            throw new ParamNotExistsException("executeUser");
                        }
                    }
                }
                ParamMappingVo protocolParamMappingVo = config.getProtocol();
                if (needProtocol && protocolParamMappingVo != null) {
                    if (protocolParamMappingVo.getValue() != null) {
                        executeConfigVo.setProtocolId((Long) protocolParamMappingVo.getValue());
                    } else {
                        Long protocol = paramObj.getLong("protocol");
                        if (protocol != null) {
                            executeConfigVo.setProtocolId(protocol);
                        } else {
                            throw new ParamNotExistsException("protocol");
                        }
                    }
                }
                ParamMappingVo executeNodeParamMappingVo = config.getExecuteNodeConfig();
                if (needExecuteNode && executeNodeParamMappingVo != null) {
                    if (executeNodeParamMappingVo.getValue() != null) {
                        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = JSONObject.toJavaObject((JSONObject) executeNodeParamMappingVo.getValue(), AutoexecCombopExecuteNodeConfigVo.class);
                        executeConfigVo.setExecuteNodeConfig(executeNodeConfig);
                    } else {
                        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = paramObj.getObject("executeNodeConfig", AutoexecCombopExecuteNodeConfigVo.class);
                        if (executeNodeConfig.isNull()) {
                            throw new ParamNotExistsException("executeNodeConfig");
                        } else {
                            executeConfigVo.setExecuteNodeConfig(executeNodeConfig);
                        }
                    }
                }
                autoexecJobVo.setExecuteConfig(executeConfigVo);
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
                            }
                        }
                    }
                    JSONObject runtimeParamMap = paramObj.getJSONObject("runtimeParamMap");
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
                autoexecJobVo.setParam(param);
            }
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobVo);
        autoexecJobActionService.settingJobFireMode(autoexecJobVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", autoexecJobVo.getId());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "autoexec/service/job/create";
    }
}
