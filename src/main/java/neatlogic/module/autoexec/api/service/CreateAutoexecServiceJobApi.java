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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.form.exception.FormAttributeRequiredException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
    private AuthenticationInfoService authenticationInfoService;

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
//        Map<Long, AutoexecCombopGroupVo> groupMap = versionConfigVo.getCombopGroupList().stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
//        List<AutoexecCombopPhaseVo> combopPhaseList = versionConfigVo.getCombopPhaseList();
//        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
//            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
//                autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo, combopPhaseVo, groupMap.get(combopPhaseVo.getGroupId()));
//            }
//        }
        autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo);
        boolean needExecuteUser = autoexecCombopVersionVo.getNeedExecuteUser();
        boolean needExecuteNode = autoexecCombopVersionVo.getNeedExecuteNode();
        boolean needProtocol = autoexecCombopVersionVo.getNeedProtocol();
        boolean needRoundCount = autoexecCombopVersionVo.getNeedRoundCount();
        // 如果服务编辑页设置了表单，且分批数量、执行目标、连接协议、执行帐号、作业参数是必填时，要么映射表单组件，要么映射常量（必填）。
        // 如果服务编辑页没有设置了表单，那么分批数量、执行目标、连接协议、执行帐号、作业参数等可填也可不填，不填的话，在服务创建作业时再填。
        String formUuid = autoexecServiceVo.getFormUuid();
        if (StringUtils.isNotBlank(formUuid)) {
            JSONArray formAttributeDataList = paramObj.getJSONArray("formAttributeDataList");
            JSONArray hidecomponentList = paramObj.getJSONArray("hidecomponentList");
            if (CollectionUtils.isEmpty(formAttributeDataList)) {
                throw new ParamNotExistsException("表单属性数据列表（formAttributeDataList）");
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
            FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(formUuid);
            List<FormAttributeVo> formAttributeVoList = formVersionVo.getFormAttributeList();
            for (FormAttributeVo formAttributeVo : formAttributeVoList) {
                String uuid = formAttributeVo.getUuid();
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
                                    param.put(key, formAttrValue);
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
                        throw new ParamNotExistsException("作业参数[" + autoexecParamVo.getName() + "]（" + autoexecParamVo.getKey() + "）");
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
                            throw new ParamNotExistsException("分批数量（roundCount）");
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
                            throw new ParamNotExistsException("执行用户（executeUser）");
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
                            throw new ParamNotExistsException("协议ID（protocol）");
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
                            throw new ParamNotExistsException("执行目标（executeNodeConfig）");
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
                        throw new ParamNotExistsException("作业参数[" + autoexecParamVo.getName() + "]（" + autoexecParamVo.getKey() + "）");
                    }
                }
                autoexecJobVo.setParam(param);
            }
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", autoexecJobVo.getId());
        //如果是自动开始且计划开始时间小于等于当前时间则直接激活作业
        if (Objects.equals(JobTriggerType.AUTO.getValue(), triggerType) && (planStartTime != null && planStartTime <= System.currentTimeMillis())) {
            fireJob(autoexecJobVo);
            return resultObj;
        }

        if (triggerType != null) {
            // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
            if (JobTriggerType.AUTO.getValue().equals(triggerType)) {
                if (planStartTime == null) {
                    throw new ParamIrregularException("planStartTime");
                }
                IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
                if (jobHandler == null) {
                    throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
                }
                JobObject.Builder jobObjectBuilder = new JobObject.Builder(autoexecJobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
                jobHandler.reloadJob(jobObjectBuilder.build());
            }
        } else {
            fireJob(autoexecJobVo);
        }
        return resultObj;
    }

    @Override
    public String getToken() {
        return "autoexec/service/job/create";
    }

    private void fireJob(AutoexecJobVo autoexecJobParam) throws Exception {
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        autoexecJobParam.setAction(JobAction.FIRE.getValue());
        autoexecJobParam.setIsFirstFire(1);
        fireAction.doService(autoexecJobParam);
    }
}
