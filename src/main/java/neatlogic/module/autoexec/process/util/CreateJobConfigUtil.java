/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.process.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.CombopNodeSpecify;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.form.attribute.core.FormAttributeDataConversionHandlerFactory;
import neatlogic.framework.form.attribute.core.IFormAttributeDataConversionHandler;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.process.condition.core.ProcessTaskConditionFactory;
import neatlogic.framework.process.constvalue.AutoExecJobProcessSource;
import neatlogic.framework.process.constvalue.ConditionProcessTaskOptions;
import neatlogic.framework.process.crossover.IProcessTaskCrossoverService;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.util.FormUtil;
import neatlogic.module.autoexec.process.dto.CreateJobConfigConfigVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigFilterVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigMappingGroupVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigMappingVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CreateJobConfigUtil {

    /**
     * 根据工单步骤配置信息创建AutoexecJobVo对象
     *
     * @param currentProcessTaskStepVo
     * @param createJobConfigConfigVo
     * @return
     */
    public static List<AutoexecJobVo> createAutoexecJobList(ProcessTaskStepVo currentProcessTaskStepVo, CreateJobConfigConfigVo createJobConfigConfigVo, AutoexecCombopVersionVo autoexecCombopVersionVo) {
        Long processTaskId = currentProcessTaskStepVo.getProcessTaskId();
        // 如果工单有表单信息，则查询出表单配置及数据
        Map<String, Object> formAttributeDataMap = new HashMap<>();
        Map<String, Object> originalFormAttributeDataMap = new HashMap<>();
        IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
        List<FormAttributeVo> formAttributeList = processTaskCrossoverService.getFormAttributeListByProcessTaskIdAngTag(processTaskId, createJobConfigConfigVo.getFormTag());
        if (CollectionUtils.isNotEmpty(formAttributeList)) {
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskIdAndTag(processTaskId, createJobConfigConfigVo.getFormTag());
            System.out.println("processTaskFormAttributeDataList = " + JSON.toJSONString(processTaskFormAttributeDataList));
            for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                originalFormAttributeDataMap.put(attributeDataVo.getAttributeUuid(), attributeDataVo.getDataObj());
                // 放入表单普通组件数据
                if (!Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLEINPUTER.getHandler())
                        && !Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMSUBASSEMBLY.getHandler())
                        && !Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLESELECTOR.getHandler())) {
                    IFormAttributeDataConversionHandler handler = FormAttributeDataConversionHandlerFactory.getHandler(attributeDataVo.getHandler());
                    if (handler != null) {
                        formAttributeDataMap.put(attributeDataVo.getAttributeUuid(), handler.getSimpleValue(attributeDataVo.getDataObj()));
                    } else {
                        formAttributeDataMap.put(attributeDataVo.getAttributeUuid(), attributeDataVo.getDataObj());
                    }
                }
            }
            // 添加表格组件中的子组件到组件列表中
            List<FormAttributeVo> allDownwardFormAttributeList = new ArrayList<>();
            for (FormAttributeVo formAttributeVo : formAttributeList) {
                JSONObject componentObj = new JSONObject();
                componentObj.put("handler", formAttributeVo.getHandler());
                componentObj.put("uuid", formAttributeVo.getUuid());
                componentObj.put("label", formAttributeVo.getLabel());
                componentObj.put("config", formAttributeVo.getConfig());
                componentObj.put("type", formAttributeVo.getType());
                List<FormAttributeVo> downwardFormAttributeList = FormUtil.getFormAttributeList(componentObj, null);
                for (FormAttributeVo downwardFormAttribute : downwardFormAttributeList) {
                    if (Objects.equals(formAttributeVo.getUuid(), downwardFormAttribute.getUuid())) {
                        continue;
                    }
                    allDownwardFormAttributeList.add(downwardFormAttribute);
                }
            }
            formAttributeList.addAll(allDownwardFormAttributeList);
        }

        JSONObject processTaskParam = ProcessTaskConditionFactory.getConditionParamData(Arrays.stream(ConditionProcessTaskOptions.values()).map(ConditionProcessTaskOptions::getValue).collect(Collectors.toList()), currentProcessTaskStepVo);
        // 作业策略createJobPolicy为single时表示单次创建作业，createJobPolicy为batch时表示批量创建作业
        String createPolicy = createJobConfigConfigVo.getCreatePolicy();
        if (Objects.equals(createPolicy, "single")) {
            AutoexecJobVo jobVo = createSingleAutoexecJobVo(currentProcessTaskStepVo, createJobConfigConfigVo, autoexecCombopVersionVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
            List<AutoexecJobVo> resultList = new ArrayList<>();
            resultList.add(jobVo);
            return resultList;
        } else if (Objects.equals(createPolicy, "batch")) {
            List<AutoexecJobVo> jobVoList = createBatchAutoexecJobVo(currentProcessTaskStepVo, createJobConfigConfigVo, autoexecCombopVersionVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
            return jobVoList;
        } else {
            return null;
        }
    }

    /**
     * 批量创建作业
     *
     * @param currentProcessTaskStepVo
     * @param createJobConfigConfigVo
     * @param formAttributeList
     * @param originalFormAttributeDataMap
     * @param formAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private static List<AutoexecJobVo> createBatchAutoexecJobVo(
            ProcessTaskStepVo currentProcessTaskStepVo,
            CreateJobConfigConfigVo createJobConfigConfigVo,
            AutoexecCombopVersionVo autoexecCombopVersionVo,
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam) {

        List<AutoexecJobVo> resultList = new ArrayList<>();
        // 批量遍历表格
        CreateJobConfigMappingVo batchDataSourceMapping = createJobConfigConfigVo.getBatchDataSourceMapping();
        if (batchDataSourceMapping == null) {
            return resultList;
        }
        JSONArray tbodyList = parseFormTableComponentMappingMode(batchDataSourceMapping, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
        if (CollectionUtils.isEmpty(tbodyList)) {
            return resultList;
        }
        // 遍历表格数据，创建AutoexecJobVo对象列表
        for (Object obj : tbodyList) {
            formAttributeDataMap.put(batchDataSourceMapping.getValue().toString(), Collections.singletonList(obj));
            AutoexecJobVo jobVo = createSingleAutoexecJobVo(currentProcessTaskStepVo, createJobConfigConfigVo, autoexecCombopVersionVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
            resultList.add(jobVo);
        }
        return resultList;
    }

    /**
     * 单次创建作业
     *
     * @param currentProcessTaskStepVo
     * @param createJobConfigConfigVo
     * @param formAttributeList
     * @param originalFormAttributeDataMap
     * @param formAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private static AutoexecJobVo createSingleAutoexecJobVo(
            ProcessTaskStepVo currentProcessTaskStepVo,
            CreateJobConfigConfigVo createJobConfigConfigVo,
            AutoexecCombopVersionVo autoexecCombopVersionVo,
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam) {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        // 组合工具ID
        Long combopId = createJobConfigConfigVo.getCombopId();
        // 作业名称
        String jobName = createJobConfigConfigVo.getJobName();
        AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
        // 场景
        if (CollectionUtils.isNotEmpty(versionConfig.getScenarioList())) {
            List<CreateJobConfigMappingGroupVo> scenarioParamMappingGroupList = createJobConfigConfigVo.getScenarioParamMappingGroupList();
            if (CollectionUtils.isNotEmpty(scenarioParamMappingGroupList)) {
                JSONArray jsonArray = parseCreateJobConfigMappingGroup(scenarioParamMappingGroupList.get(0), formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                Long scenarioId = getScenarioId(jsonArray, versionConfig.getScenarioList());
                jobVo.setScenarioId(scenarioId);
            }
        }
        if (CollectionUtils.isNotEmpty(versionConfig.getRuntimeParamList())) {
            List<AutoexecParamVo> jobParamList = versionConfig.getRuntimeParamList();
            Map<String, AutoexecParamVo> jobParamMap = jobParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getKey, e -> e));
            // 作业参数赋值列表
            List<CreateJobConfigMappingGroupVo> jopParamMappingGroupList = createJobConfigConfigVo.getJobParamMappingGroupList();
            if (CollectionUtils.isNotEmpty(jopParamMappingGroupList)) {
                JSONObject param = new JSONObject();
                for (CreateJobConfigMappingGroupVo mappingGroupVo : jopParamMappingGroupList) {
                    AutoexecParamVo autoexecParamVo = jobParamMap.get(mappingGroupVo.getKey());
                    if (autoexecParamVo == null) {
                        continue;
                    }
                    JSONArray jsonArray = parseCreateJobConfigMappingGroup(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                    if (CollectionUtils.isEmpty(jsonArray)) {
                        continue;
                    }
                    Object value = convertDateType(autoexecParamVo, jsonArray);
                    param.put(mappingGroupVo.getKey(), value);
                }
                jobVo.setParam(param);
            }
        }

        // 目标参数赋值列表
        Map<String, CreateJobConfigMappingGroupVo> executeParamMappingGroupMap = new HashMap<>();
        List<CreateJobConfigMappingGroupVo> executeParamMappingGroupList = createJobConfigConfigVo.getExecuteParamMappingGroupList();
        if (CollectionUtils.isNotEmpty(executeParamMappingGroupList)) {
            executeParamMappingGroupMap = executeParamMappingGroupList.stream().collect(Collectors.toMap(CreateJobConfigMappingGroupVo::getKey, e -> e));
        }
        IAutoexecCombopCrossoverService autoexecCombopCrossoverService = CrossoverServiceFactory.getApi(IAutoexecCombopCrossoverService.class);
        autoexecCombopCrossoverService.needExecuteConfig(autoexecCombopVersionVo);
        // 流程图自动化节点是否需要设置执行用户，只有当有某个非runner类型的阶段，没有设置执行用户时，needExecuteUser=true
        boolean needExecuteUser = autoexecCombopVersionVo.getNeedExecuteUser();
        // 流程图自动化节点是否需要设置连接协议，只有当有某个非runner类型的阶段，没有设置连接协议时，needProtocol=true
        boolean needProtocol = autoexecCombopVersionVo.getNeedProtocol();
        // 流程图自动化节点是否需要设置执行目标，只有当有某个非runner类型的阶段，没有设置执行目标时，needExecuteNode=true
        boolean needExecuteNode = autoexecCombopVersionVo.getNeedExecuteNode();
        // 流程图自动化节点是否需要设置分批数量，只有当有某个非runner类型的阶段，没有设置分批数量时，needRoundCount=true
        boolean needRoundCount = autoexecCombopVersionVo.getNeedRoundCount();
        AutoexecCombopExecuteConfigVo combopExecuteConfig = versionConfig.getExecuteConfig();
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        if (needExecuteNode) {
            String whenToSpecify = combopExecuteConfig.getWhenToSpecify();
            if (Objects.equals(CombopNodeSpecify.NOW.getValue(), whenToSpecify)) {
                AutoexecCombopExecuteNodeConfigVo executeNodeConfig = combopExecuteConfig.getExecuteNodeConfig();
                if (executeNodeConfig != null) {
                    executeConfig.setExecuteNodeConfig(executeNodeConfig);
                }
            } else if (Objects.equals(CombopNodeSpecify.RUNTIMEPARAM.getValue(), whenToSpecify)) {
                AutoexecCombopExecuteNodeConfigVo executeNodeConfig = combopExecuteConfig.getExecuteNodeConfig();
                if (executeNodeConfig != null) {
                    executeConfig.setExecuteNodeConfig(executeNodeConfig);
//                    List<String> paramList = executeNodeConfig.getParamList();
//                    if (CollectionUtils.isNotEmpty(paramList)) {
//                        List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
//                        JSONObject paramObj = jobVo.getParam();
//                        for (String paramKey : paramList) {
//                            JSONArray jsonArray = paramObj.getJSONArray(paramKey);
//                            if (CollectionUtils.isNotEmpty(jsonArray)) {
//                                List<AutoexecNodeVo> list = jsonArray.toJavaList(AutoexecNodeVo.class);
//                                inputNodeList.addAll(list);
//                            }
//                        }
//                        if (CollectionUtils.isNotEmpty(inputNodeList)) {
//                            AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
//                            executeNodeConfigVo.setInputNodeList(inputNodeList);
//                            executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
//                        }
//                    }
                }
            } else if (Objects.equals(CombopNodeSpecify.RUNTIME.getValue(), whenToSpecify)) {
                CreateJobConfigMappingGroupVo mappingGroupVo = executeParamMappingGroupMap.get("executeNodeConfig");
                if (mappingGroupVo != null) {
                    JSONArray jsonArray = parseCreateJobConfigMappingGroup(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                    AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = getExecuteNodeConfig(jsonArray);
                    if (executeNodeConfigVo != null) {
                        executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
                    }
//                    List<AutoexecNodeVo> inputNodeList = getInputNodeList(jsonArray);
//                    if (CollectionUtils.isNotEmpty(inputNodeList)) {
//                        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
//                        executeNodeConfigVo.setInputNodeList(inputNodeList);
//                        executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
//                    }
                }
            }
        }
        if (needProtocol) {
            if (combopExecuteConfig.getProtocolId() != null) {
                executeConfig.setProtocolId(combopExecuteConfig.getProtocolId());
            } else {
                CreateJobConfigMappingGroupVo mappingGroupVo = executeParamMappingGroupMap.get("protocolId");
                if (mappingGroupVo != null) {
                    JSONArray jsonArray = parseCreateJobConfigMappingGroup(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                    Long protocolId = getProtocolId(jsonArray);
                    executeConfig.setProtocolId(protocolId);
                }
            }
        }
        if (needExecuteUser) {
            ParamMappingVo executeUserMappingVo = combopExecuteConfig.getExecuteUser();
            if (executeUserMappingVo != null && StringUtils.isNotBlank((String) executeUserMappingVo.getValue())) {
                executeConfig.setExecuteUser(executeUserMappingVo);
            } else {
                CreateJobConfigMappingGroupVo mappingGroupVo = executeParamMappingGroupMap.get("executeUser");
                if (mappingGroupVo != null) {
                    JSONArray jsonArray = parseCreateJobConfigMappingGroup(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                    String executeUser = getFirstNotBlankString(jsonArray);
                    if (StringUtils.isNotBlank(executeUser)) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        paramMappingVo.setMappingMode("constant");
                        paramMappingVo.setValue(executeUser);
                        executeConfig.setExecuteUser(paramMappingVo);
                    }
                }
            }
        }
        if (needRoundCount) {
            if (combopExecuteConfig.getRoundCount() != null) {
                executeConfig.setRoundCount(combopExecuteConfig.getRoundCount());
            } else {
                CreateJobConfigMappingGroupVo mappingGroupVo = executeParamMappingGroupMap.get("roundCount");
                if (mappingGroupVo != null) {
                    JSONArray jsonArray = parseCreateJobConfigMappingGroup(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                    Integer roundCount = getFirstNotBlankInteger(jsonArray);
                    if (roundCount != null) {
                        executeConfig.setRoundCount(roundCount);
                    }
                }
            }
        }
        jobVo.setExecuteConfig(executeConfig);

        // 执行器组
        ParamMappingVo runnerGroup = combopExecuteConfig.getRunnerGroup();
        if (runnerGroup != null) {
            jobVo.setRunnerGroup(runnerGroup);
        }

        String jobNamePrefixMappingValue = createJobConfigConfigVo.getJobNamePrefixMappingValue();
        String jobNamePrefixValue = getJobNamePrefix(jobNamePrefixMappingValue, jobVo.getExecuteConfig(), jobVo.getParam());

        jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
        jobVo.setRoundCount(32);
        jobVo.setOperationId(combopId);
        jobVo.setName(jobNamePrefixValue + jobName);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setInvokeId(currentProcessTaskStepVo.getId());
        jobVo.setRouteId(currentProcessTaskStepVo.getId().toString());
        jobVo.setIsFirstFire(1);
        jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());

        return jobVo;
    }

    private static Long getScenarioId(JSONArray jsonArray, List<AutoexecCombopScenarioVo> scenarioList) {
        if (CollectionUtils.isEmpty(jsonArray)) {
            return null;
        }
        List<Long> scenarioIdList = new ArrayList<>();
        Map<String, Long> scenarioNameToIdMap = new HashMap<>();
        for (AutoexecCombopScenarioVo combopScenarioVo : scenarioList) {
            scenarioIdList.add(combopScenarioVo.getScenarioId());
            scenarioNameToIdMap.put(combopScenarioVo.getScenarioName(), combopScenarioVo.getScenarioId());
        }
        for (Object obj : jsonArray) {
            if (obj instanceof Long) {
                Long scenarioId = (Long) obj;
                if (scenarioIdList.contains(scenarioId)) {
                    return scenarioId;
                }
            } else if (obj instanceof String) {
                String scenario = (String) obj;
                try {
                    Long scenarioId = Long.valueOf(scenario);
                    if (scenarioIdList.contains(scenarioId)) {
                        return scenarioId;
                    }
                } catch (NumberFormatException ignored) {
                    Long scenarioId = scenarioNameToIdMap.get(scenario);
                    if (scenarioId != null) {
                        return scenarioId;
                    }
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                Long scenarioId = getScenarioId(array, scenarioList);
                if (scenarioId != null) {
                    return scenarioId;
                }
            }
        }
        return null;
    }

    private static Long getProtocolId(JSONArray jsonArray) {
        if (CollectionUtils.isEmpty(jsonArray)) {
            return null;
        }
        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
        for (Object obj : jsonArray) {
            if (obj instanceof Long) {
                Long protocolId = (Long) obj;
                AccountProtocolVo accountProtocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolId(protocolId);
                if (accountProtocolVo != null) {
                    return protocolId;
                }
            } else if (obj instanceof String) {
                String protocol = (String) obj;
                try {
                    Long protocolId = Long.valueOf(protocol);
                    AccountProtocolVo accountProtocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolId(protocolId);
                    if (accountProtocolVo != null) {
                        return protocolId;
                    }
                } catch (NumberFormatException ex) {
                    AccountProtocolVo accountProtocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolName(protocol);
                    if (accountProtocolVo != null) {
                        return accountProtocolVo.getId();
                    }
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                Long protocolId = getProtocolId(array);
                if (protocolId != null) {
                    return protocolId;
                }
            }
        }
        return null;
    }

    private static Long getAccountId(JSONArray jsonArray) {
        if (CollectionUtils.isEmpty(jsonArray)) {
            return null;
        }
        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
        for (Object obj : jsonArray) {
            if (obj instanceof Long) {
                Long accountId = (Long) obj;
                AccountVo accountVo = resourceAccountCrossoverMapper.getAccountById(accountId);
                if (accountVo != null) {
                    return accountId;
                }
            } else if (obj instanceof String) {
                String account = (String) obj;
                try {
                    Long accountId = Long.valueOf(account);
                    AccountVo accountVo = resourceAccountCrossoverMapper.getAccountById(accountId);
                    if (accountVo != null) {
                        return accountId;
                    }
                } catch (NumberFormatException ex) {
                    AccountVo accountVo = resourceAccountCrossoverMapper.getPublicAccountByName(account);
                    if (accountVo != null) {
                        return accountVo.getId();
                    }
                }
            } else if (obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                Long accountId = jsonObj.getLong("accountId");
                AccountVo accountVo = resourceAccountCrossoverMapper.getAccountById(accountId);
                if (accountVo != null) {
                    return accountId;
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                Long accountId = getAccountId(array);
                if (accountId != null) {
                    return accountId;
                }
            }
        }
        return null;
    }

    private static AutoexecCombopExecuteNodeConfigVo getExecuteNodeConfig(JSONArray jsonArray) {
        if (CollectionUtils.isEmpty(jsonArray)) {
            return null;
        }
        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
        List<AutoexecNodeVo> selectNodeList = new ArrayList<>();
        List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
        JSONObject filter = new JSONObject();
        for (Object obj : jsonArray) {
            if (obj instanceof String) {
                String str = (String) obj;
                if (str.startsWith("{") && str.endsWith("}")) {
                    JSONObject jsonObj = JSON.parseObject(str);
                    String ip = jsonObj.getString("ip");
                    if (StringUtils.isNotBlank(ip)) {
                        inputNodeList.add(convertToAutoexecNodeVo(jsonObj));
                    }
                } else if (str.startsWith("[") && str.endsWith("]")) {
                    JSONArray array = JSON.parseArray(str);
                    List<AutoexecNodeVo> list = getInputNodeList(array);
                    if (CollectionUtils.isNotEmpty(list)) {
                        inputNodeList.addAll(list);
                    }
                } else if (str.contains("\n")) {
                    String[] split = str.split("\n");
                    for (String e : split) {
                        inputNodeList.add(new AutoexecNodeVo(e));
                    }
                } else {
                    inputNodeList.add(new AutoexecNodeVo(str));
                }
            } else if (obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                JSONArray selectNodeArray = jsonObj.getJSONArray("selectNodeList");
                if (CollectionUtils.isNotEmpty(selectNodeArray)) {
                    selectNodeList.addAll(selectNodeArray.toJavaList(AutoexecNodeVo.class));
                }
                JSONArray inputNodeArray = jsonObj.getJSONArray("inputNodeList");
                if (CollectionUtils.isNotEmpty(inputNodeArray)) {
                    inputNodeList.addAll(inputNodeArray.toJavaList(AutoexecNodeVo.class));
                }
                JSONObject filterObj = jsonObj.getJSONObject("filter");
                if (MapUtils.isNotEmpty(filterObj)) {
                    filter.putAll(filterObj);
                }
                String ip = jsonObj.getString("ip");
                if (StringUtils.isNotBlank(ip)) {
                    inputNodeList.add(convertToAutoexecNodeVo(jsonObj));
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                List<AutoexecNodeVo> list = getInputNodeList(array);
                if (CollectionUtils.isNotEmpty(list)) {
                    inputNodeList.addAll(list);
                }
            }
        }
        executeNodeConfigVo.setSelectNodeList(selectNodeList);
        executeNodeConfigVo.setInputNodeList(inputNodeList);
        executeNodeConfigVo.setFilter(filter);
        return executeNodeConfigVo;
    }

    private static List<AutoexecNodeVo> getInputNodeList(JSONArray jsonArray) {
        List<AutoexecNodeVo> resultList = new ArrayList<>();
        if (CollectionUtils.isEmpty(jsonArray)) {
            return resultList;
        }
        for (Object obj : jsonArray) {
            if (obj instanceof String) {
                String str = (String) obj;
                if (str.startsWith("{") && str.endsWith("}")) {
                    JSONObject jsonObj = JSON.parseObject(str);
                    String ip = jsonObj.getString("ip");
                    if (StringUtils.isNotBlank(ip)) {
                        resultList.add(convertToAutoexecNodeVo(jsonObj));
                    }
                } else if (str.startsWith("[") && str.endsWith("]")) {
                    JSONArray array = JSON.parseArray(str);
                    List<AutoexecNodeVo> list = getInputNodeList(array);
                    if (CollectionUtils.isNotEmpty(list)) {
                        resultList.addAll(list);
                    }
                } else if (str.contains("\n")) {
                    String[] split = str.split("\n");
                    for (String e : split) {
                        resultList.add(new AutoexecNodeVo(e));
                    }
                } else {
                    resultList.add(new AutoexecNodeVo(str));
                }
            } else if (obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                JSONArray selectNodeArray = jsonObj.getJSONArray("selectNodeList");
                if (CollectionUtils.isNotEmpty(selectNodeArray)) {
                    resultList.addAll(selectNodeArray.toJavaList(AutoexecNodeVo.class));
                }
                JSONArray inputNodeArray = jsonObj.getJSONArray("inputNodeList");
                if (CollectionUtils.isNotEmpty(inputNodeArray)) {
                    resultList.addAll(inputNodeArray.toJavaList(AutoexecNodeVo.class));
                }
                String ip = jsonObj.getString("ip");
                if (StringUtils.isNotBlank(ip)) {
                    resultList.add(convertToAutoexecNodeVo(jsonObj));
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                List<AutoexecNodeVo> list = getInputNodeList(array);
                if (CollectionUtils.isNotEmpty(list)) {
                    resultList.addAll(list);
                }
            }
        }
        return resultList;
    }

    private static AutoexecNodeVo convertToAutoexecNodeVo(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        String ip = jsonObj.getString("ip");
        Integer port = jsonObj.getInteger("port");
        String name = jsonObj.getString("name");
        AutoexecNodeVo autoexecNodeVo = new AutoexecNodeVo();
        autoexecNodeVo.setIp(ip);
        if (id != null) {
            autoexecNodeVo.setId(id);
        }
        if (port != null) {
            autoexecNodeVo.setPort(port);
        }
        if (StringUtils.isNotBlank(name)) {
            autoexecNodeVo.setName(name);
        }
        return autoexecNodeVo;
    }

    /**
     * 根据设置找到作业名称前缀值
     *
     * @param jobNamePrefixMappingValue 作业名称前缀映射值
     * @param executeConfig    目标参数
     * @param param            作业参数
     * @return 返回作业名称前缀值
     */
    private static String getJobNamePrefix(String jobNamePrefixMappingValue, AutoexecCombopExecuteConfigVo executeConfig, JSONObject param) {
        String jobNamePrefixValue = StringUtils.EMPTY;
        if (StringUtils.isBlank(jobNamePrefixMappingValue)) {
            return jobNamePrefixValue;
        }
        if (Objects.equals(jobNamePrefixMappingValue, "executeNodeConfig")) {
            AutoexecCombopExecuteNodeConfigVo executeNodeConfig = executeConfig.getExecuteNodeConfig();
            List<AutoexecNodeVo> inputNodeList = executeNodeConfig.getInputNodeList();
            List<AutoexecNodeVo> selectNodeList = executeNodeConfig.getSelectNodeList();
            List<String> paramList = executeNodeConfig.getParamList();
            if (CollectionUtils.isNotEmpty(inputNodeList)) {
                List<String> list = new ArrayList<>();
                for (AutoexecNodeVo node : inputNodeList) {
                    list.add(node.toString());
                }
                jobNamePrefixValue = String.join("", list);
            } else if (CollectionUtils.isNotEmpty(selectNodeList)) {
                List<String> list = new ArrayList<>();
                for (AutoexecNodeVo node : selectNodeList) {
                    list.add(node.toString());
                }
                jobNamePrefixValue = String.join("", list);
            } else if (CollectionUtils.isNotEmpty(paramList)) {
                List<String> list = new ArrayList<>();
                for (String paramKey : paramList) {
                    Object value = param.get(paramKey);
                    if (value != null) {
                        if (value instanceof String) {
                            list.add((String) value);
                        } else {
                            list.add(JSONObject.toJSONString(value));
                        }
                    }
                }
                jobNamePrefixValue = String.join("", list);
            }
        } else if (Objects.equals(jobNamePrefixMappingValue, "executeUser")) {
            ParamMappingVo executeUser = executeConfig.getExecuteUser();
            if (executeUser != null) {
                Object value = executeUser.getValue();
                if (value != null) {
                    if (Objects.equals(executeUser.getMappingMode(), "runtimeparam")) {
                        value = param.get(value);
                    }
                    if (value != null) {
                        if (value instanceof String) {
                            jobNamePrefixValue = (String) value;
                        } else {
                            jobNamePrefixValue = JSONObject.toJSONString(value);
                        }
                    }
                }
            }
        } else if (Objects.equals(jobNamePrefixMappingValue, "protocolId")) {
            Long protocolId = executeConfig.getProtocolId();
            if (protocolId != null) {
                jobNamePrefixValue = protocolId.toString();
            }
        } else if (Objects.equals(jobNamePrefixMappingValue, "roundCount")) {
            Integer roundCount = executeConfig.getRoundCount();
            if (roundCount != null) {
                jobNamePrefixValue = roundCount.toString();
            }
        } else {
            Object jobNamePrefixObj = param.get(jobNamePrefixMappingValue);
            if (jobNamePrefixObj instanceof String) {
                jobNamePrefixValue = (String) jobNamePrefixObj;
            } else {
                jobNamePrefixValue = JSONObject.toJSONString(jobNamePrefixObj);
            }
        }
        if (StringUtils.isBlank(jobNamePrefixValue)) {
            return StringUtils.EMPTY;
        } else if (jobNamePrefixValue.length() > 32) {
            return jobNamePrefixValue.substring(0, 32);
        }
        return jobNamePrefixValue;
    }

    private static JSONArray parseCreateJobConfigMappingGroup(CreateJobConfigMappingGroupVo mappingGroupVo,
                                                              List<FormAttributeVo> formAttributeList,
                                                              Map<String, Object> originalFormAttributeDataMap,
                                                              Map<String, Object> formAttributeDataMap,
                                                              JSONObject processTaskParam) {
        JSONArray resultList = new JSONArray();
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return resultList;
        }
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            Object value = mappingVo.getValue();
            if (value == null) {
                continue;
            }
            String mappingMode = mappingVo.getMappingMode();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                resultList.add(parseFormTableComponentMappingMode(mappingVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam));
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                resultList.add(formAttributeDataMap.get(value));
            } else if (Objects.equals(mappingMode, "constant")) {
                resultList.add(value);
            } else if (Objects.equals(mappingMode, "processTaskParam")) {
                resultList.add(processTaskParam.get(value));
            } else if (Objects.equals(mappingMode, "expression")) {
                if (value instanceof JSONArray) {
                    resultList.add(parseExpression((JSONArray) value, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam));
                }
            }
        }
        return resultList;
    }

    /**
     * 解析表单表格组件映射模式，得到映射结果
     * @param mappingVo
     * @param formAttributeList
     * @param originalFormAttributeDataMap
     * @param formAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private static JSONArray parseFormTableComponentMappingMode(CreateJobConfigMappingVo mappingVo,
                                                         List<FormAttributeVo> formAttributeList,
                                                         Map<String, Object> originalFormAttributeDataMap,
                                                         Map<String, Object> formAttributeDataMap,
                                                         Map<String, Object> processTaskParam) {
        JSONArray resultList = new JSONArray();
        List<JSONObject> mainTableDataList = getFormTableComponentData(formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, mappingVo.getValue().toString());
        if (CollectionUtils.isEmpty(mainTableDataList)) {
            return resultList;
        }
        List<CreateJobConfigFilterVo> filterList = mappingVo.getFilterList();
        if (CollectionUtils.isNotEmpty(filterList)) {
            List<JSONObject> totalDerivedTableDataList = new ArrayList<>();
            for (JSONObject rowData : mainTableDataList) {
                JSONObject newRowData = new JSONObject();
                for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                    newRowData.put(mappingVo.getValue() + "." + entry.getKey(), entry.getValue());
                }
                totalDerivedTableDataList.add(newRowData);
            }
            for (CreateJobConfigFilterVo filterVo : filterList) {
                if (CollectionUtils.isEmpty(totalDerivedTableDataList)) {
                    break;
                }
                List<JSONObject> derivedTableDataList = new ArrayList<>();
                if (Objects.equals(filterVo.getLeftMappingMode() ,"formTableComponent")) {
                    if (Objects.equals(filterVo.getRightMappingMode() ,"formTableComponent")) {
                        boolean flag = false;
                        for (JSONObject rowData : totalDerivedTableDataList) {
                            if (rowData.containsKey(filterVo.getRightValue() + ".")) {
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            List<JSONObject> rightTableDataList = getFormTableComponentData(formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, filterVo.getRightValue());
                            if (CollectionUtils.isNotEmpty(rightTableDataList)) {
                                for (JSONObject rowData : totalDerivedTableDataList) {
                                    Object leftData = rowData.get(filterVo.getLeftValue() + "." + filterVo.getLeftColumn());
                                    for (JSONObject rightRowData : rightTableDataList) {
                                        Object rightData = rightRowData.get(filterVo.getRightColumn());
                                        if (expressionAssert(leftData, filterVo.getExpression(), rightData)) {
                                            JSONObject newRowData = new JSONObject();
                                            newRowData.putAll(rowData);
                                            for (Map.Entry<String, Object> entry : rightRowData.entrySet()) {
                                                newRowData.put(filterVo.getRightValue() + "." + entry.getKey(), entry.getValue());
                                            }
                                            derivedTableDataList.add(newRowData);
                                        }
                                    }
                                }
                            }
                        } else {
                            for (JSONObject rowData : totalDerivedTableDataList) {
                                Object leftData = rowData.get(filterVo.getLeftValue() + "." + filterVo.getLeftColumn());
                                Object rightData = rowData.get(filterVo.getRightValue() + "." + filterVo.getRightColumn());
                                if (expressionAssert(leftData, filterVo.getExpression(), rightData)) {
                                    derivedTableDataList.add(rowData);
                                }
                            }
                        }
                    } else if (Objects.equals(filterVo.getRightMappingMode() ,"formCommonComponent")) {
                        Object rightData = formAttributeDataMap.get(filterVo.getRightValue());
                        for (JSONObject rowData : totalDerivedTableDataList) {
                            Object leftData = rowData.get(filterVo.getLeftValue() + "." + filterVo.getLeftColumn());
                            if (expressionAssert(leftData, filterVo.getExpression(), rightData)) {
                                derivedTableDataList.add(rowData);
                            }
                        }
                    } else if (Objects.equals(filterVo.getRightMappingMode() ,"constant")) {
                        Object rightData = filterVo.getRightValue();
                        for (JSONObject rowData : totalDerivedTableDataList) {
                            Object leftData = rowData.get(filterVo.getLeftValue() + "." + filterVo.getLeftColumn());
                            if (expressionAssert(leftData, filterVo.getExpression(), rightData)) {
                                derivedTableDataList.add(rowData);
                            }
                        }
                    } else if (Objects.equals(filterVo.getRightMappingMode() ,"processTaskParam")) {
                        Object rightData = processTaskParam.get(filterVo.getRightValue());
                        for (JSONObject rowData : totalDerivedTableDataList) {
                            Object leftData = rowData.get(filterVo.getLeftValue() + "." + filterVo.getLeftColumn());
                            if (expressionAssert(leftData, filterVo.getExpression(), rightData)) {
                                derivedTableDataList.add(rowData);
                            }
                        }
                    } else if (Objects.equals(filterVo.getRightMappingMode() ,"expression")) {
                        Object rightData = filterVo.getRightValue();
                        for (JSONObject rowData : totalDerivedTableDataList) {
                            Object leftData = rowData.get(filterVo.getLeftValue() + "." + filterVo.getLeftColumn());
                            if (expressionAssert(leftData, filterVo.getExpression(), rightData)) {
                                derivedTableDataList.add(rowData);
                            }
                        }
                    }
                }
                totalDerivedTableDataList = derivedTableDataList;
            }
            if (CollectionUtils.isEmpty(totalDerivedTableDataList)) {
                return resultList;
            }
            List<JSONObject> derivedTableDataList = new ArrayList<>();
            for (JSONObject rowData : totalDerivedTableDataList) {
                JSONObject newRowData = new JSONObject();
                String prefix = mappingVo.getValue().toString() + ".";
                for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith(prefix)) {
                        key = key.substring(prefix.length());
                        newRowData.put(key, entry.getValue());
                    }
                }
                if (!derivedTableDataList.contains(newRowData)) {
                    derivedTableDataList.add(newRowData);
                }
            }
            mainTableDataList = derivedTableDataList;
        }
        if (StringUtils.isNotBlank(mappingVo.getColumn())) {
            for (JSONObject rowData : mainTableDataList) {
                Object obj = rowData.get(mappingVo.getColumn());
                if (obj != null) {
                    resultList.add(obj);
                }
            }
        } else {
            resultList.addAll(mainTableDataList);
        }
        if (Boolean.TRUE.equals(mappingVo.getDistinct())) {
            JSONArray tempList = new JSONArray();
            for (Object obj : resultList) {
                if (!tempList.contains(obj)) {
                    tempList.add(obj);
                }
            }
            resultList = tempList;
        }
        if (CollectionUtils.isNotEmpty(mappingVo.getLimit())) {
            Integer fromIndex = mappingVo.getLimit().get(0);
            int toIndex = resultList.size();
            if (mappingVo.getLimit().size() > 1) {
                Integer pageSize = mappingVo.getLimit().get(1);
                toIndex = fromIndex + pageSize;
                if (toIndex > resultList.size()) {
                    toIndex = resultList.size();
                }
            }
            JSONArray tempList = new JSONArray();
            for (int i = 0; i < resultList.size(); i++) {
                if (i >= fromIndex && i < toIndex) {
                    tempList.add(resultList.get(i));
                }
            }
            resultList = tempList;
        }
        return resultList;
    }

    private static boolean expressionAssert(Object leftValue, String expression, Object rightValue) {
        if (Objects.equals(expression, Expression.EQUAL.getExpression())) {
            if (Objects.equals(leftValue, rightValue) || Objects.equals(JSONObject.toJSONString(leftValue).toLowerCase(), JSONObject.toJSONString(rightValue).toLowerCase())) {
                return true;
            }
        } else if (Objects.equals(expression, Expression.UNEQUAL.getExpression())) {
            if (!Objects.equals(leftValue, rightValue) && !Objects.equals(JSONObject.toJSONString(leftValue).toLowerCase(), JSONObject.toJSONString(rightValue).toLowerCase())) {
                return true;
            }
        } else if (Objects.equals(expression, Expression.LIKE.getExpression())) {
            if (leftValue == null || rightValue == null) {
                return false;
            }
            String leftValueStr = JSONObject.toJSONString(leftValue).toLowerCase();
            String rightValueStr = JSONObject.toJSONString(rightValue).toLowerCase();
            if (leftValueStr.contains(rightValueStr)) {
                return true;
            }
        } else if (Objects.equals(expression, Expression.NOTLIKE.getExpression())) {
            if (leftValue == null || rightValue == null) {
                return false;
            }
            String leftValueStr = JSONObject.toJSONString(leftValue).toLowerCase();
            String rightValueStr = JSONObject.toJSONString(rightValue).toLowerCase();
            if (!leftValueStr.contains(rightValueStr)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取表单表格组件的数据
     * @param formAttributeList
     * @param originalFormAttributeDataMap
     * @param attributeUuid
     * @return
     */
    @SuppressWarnings("unchecked")
    private static List<JSONObject> getFormTableComponentData(
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            String attributeUuid) {
        List<JSONObject> resultList = new ArrayList<>();
        Object object = formAttributeDataMap.get(attributeUuid);
        if (object != null) {
            return (List<JSONObject>) object;
        }
        Object obj = originalFormAttributeDataMap.get(attributeUuid);
        if (obj == null) {
            return resultList;
        }
        if (!(obj instanceof JSONArray)) {
            return resultList;
        }
        JSONArray array = (JSONArray) obj;
        if (CollectionUtils.isEmpty(array)) {
            return resultList;
        }

        for (int i = 0; i < array.size(); i++) {
            JSONObject newJsonObj = new JSONObject();
            JSONObject jsonObj = array.getJSONObject(i);
            for (Map.Entry<String, Object> entry : jsonObj.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                FormAttributeVo formAttributeVo = getFormAttributeVo(formAttributeList, key);
                if (formAttributeVo != null) {
                    IFormAttributeDataConversionHandler handler = FormAttributeDataConversionHandlerFactory.getHandler(formAttributeVo.getHandler());
                    if (handler != null) {
                        value = handler.getSimpleValue(value);
                    }
                    newJsonObj.put(key, value);
                } else {
                    newJsonObj.put(key, value);
                }
            }
            resultList.add(newJsonObj);
        }
        return resultList;
    }

    private static FormAttributeVo getFormAttributeVo(List<FormAttributeVo> formAttributeList, String uuid) {
        if (CollectionUtils.isNotEmpty(formAttributeList)) {
            for (FormAttributeVo formAttributeVo : formAttributeList) {
                if (Objects.equals(formAttributeVo.getUuid(), uuid)) {
                    return formAttributeVo;
                }
            }
        }
        return null;
    }

    private static FormAttributeVo getFormAttributeVoByKeyAndParentUuid(List<FormAttributeVo> formAttributeList, String key, String parentUuid) {
        if (CollectionUtils.isNotEmpty(formAttributeList)) {
            for (FormAttributeVo formAttributeVo : formAttributeList) {
                if (Objects.equals(formAttributeVo.getKey(), key)) {
                    if (parentUuid == null) {
                        return formAttributeVo;
                    }
                    if (formAttributeVo.getParent() != null && Objects.equals(formAttributeVo.getParent().getUuid(), parentUuid)) {
                        return formAttributeVo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 解析出表达式的值
     * @param valueList
     * @param formAttributeList
     * @param originalFormAttributeDataMap
     * @param formAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private static String parseExpression(JSONArray valueList,
                                   List<FormAttributeVo> formAttributeList,
                                   Map<String, Object> originalFormAttributeDataMap,
                                   Map<String, Object> formAttributeDataMap,
                                   JSONObject processTaskParam) {
        StringBuilder stringBuilder = new StringBuilder();
        List<CreateJobConfigMappingVo> mappingList = valueList.toJavaList(CreateJobConfigMappingVo.class);
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            String value = mappingVo.getValue().toString();
            String mappingMode = mappingVo.getMappingMode();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                JSONArray array = parseFormTableComponentMappingMode(mappingVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                List<String> list = new ArrayList<>();
                for (int j = 0; j < array.size(); j++) {
                    list.add(array.getString(j));
                }
                stringBuilder.append(String.join(",", list));
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                Object obj = formAttributeDataMap.get(value);
                if (obj != null) {
                    if (obj instanceof JSONArray) {
                        List<String> list = new ArrayList<>();
                        JSONArray dataObjectArray = (JSONArray) obj;
                        for (int j = 0; j < dataObjectArray.size(); j++) {
                            list.add(dataObjectArray.getString(j));
                        }
                        stringBuilder.append(String.join(",", list));
                    } else {
                        stringBuilder.append(obj);
                    }
                }
            } else if (Objects.equals(mappingMode, "constant")) {
                stringBuilder.append(value);
            } else if (Objects.equals(mappingMode, "processTaskParam")) {
                stringBuilder.append(processTaskParam.get(value));
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 把表单表格组件中某列数据集合转换成作业参数对应的数据
     *
     * @param autoexecParamVo  作业参数信息
     * @param jsonArray 某列数据集合
     * @return
     */
    private static Object convertDateType(AutoexecParamVo autoexecParamVo, JSONArray jsonArray) {
        if (CollectionUtils.isEmpty(jsonArray)) {
            return null;
        }
        String paramType = autoexecParamVo.getType();
        if (Objects.equals(paramType, ParamType.TEXT.getValue())) {
            return String.join(",", getStringList(jsonArray));
        } else if (Objects.equals(paramType, ParamType.PASSWORD.getValue())) {
            return String.join(",", getStringList(jsonArray));
        } else if (Objects.equals(paramType, ParamType.FILE.getValue())) {
            // 多选
            return getFileInfo(jsonArray);
        } else if (Objects.equals(paramType, ParamType.DATE.getValue())) {
            return getFirstNotBlankString(jsonArray);
        } else if (Objects.equals(paramType, ParamType.DATETIME.getValue())) {
            return getFirstNotBlankString(jsonArray);
        } else if (Objects.equals(paramType, ParamType.TIME.getValue())) {
            return getFirstNotBlankString(jsonArray);
        } else if (Objects.equals(paramType, ParamType.JSON.getValue())) {
            return getJSONObjectOrJSONArray(jsonArray);
        } else if (Objects.equals(paramType, ParamType.SELECT.getValue())) {
            return getFirstNotNullObject(jsonArray);
        } else if (Objects.equals(paramType, ParamType.MULTISELECT.getValue())) {
            return getObjectList(jsonArray);
        } else if (Objects.equals(paramType, ParamType.RADIO.getValue())) {
            return getFirstNotNullObject(jsonArray);
        } else if (Objects.equals(paramType, ParamType.CHECKBOX.getValue())) {
            return getObjectList(jsonArray);
        } else if (Objects.equals(paramType, ParamType.NODE.getValue())) {
            return getInputNodeList(jsonArray);
        } else if (Objects.equals(paramType, ParamType.ACCOUNT.getValue())) {
            // 账号id，单选
            return getAccountId(jsonArray);
        } else if (Objects.equals(paramType, ParamType.USERSELECT.getValue())) {
            // 单选或多选都是数组
            return getUserSelectInfo(jsonArray);
        } else if (Objects.equals(paramType, ParamType.TEXTAREA.getValue())) {
            return String.join(",", getStringList(jsonArray));
        } else if (Objects.equals(paramType, ParamType.PHASE.getValue())) {
            // 阶段名称，单选
            return getFirstNotBlankString(jsonArray);
        } else if (Objects.equals(paramType, ParamType.SWITCH.getValue())) {
            // true或false
            Boolean bool = getFirstNotNullBoolean(jsonArray);
            if (Boolean.TRUE == bool) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else if (Objects.equals(paramType, ParamType.FILEPATH.getValue())) {
            return getFirstNotBlankString(jsonArray);
        } else if (Objects.equals(paramType, ParamType.RUNNERGROUP.getValue())) {
            // 组id，单选
            return getFirstNotNullObject(jsonArray);
        }
        return null;
    }

    private static List<String> getStringList(JSONArray jsonArray) {
        List<String> resultList = new ArrayList<>();
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                resultList.addAll(getStringList(array));
            } else {
                resultList.add(obj.toString());
            }
        }
        return resultList;
    }

    private static List<Object> getObjectList(JSONArray jsonArray) {
        List<Object> resultList = new ArrayList<>();
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                resultList.addAll(getObjectList(array));
            } else {
                resultList.add(obj);
            }
        }
        return resultList;
    }

    private static Integer getFirstNotBlankInteger(JSONArray jsonArray) {
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                Integer integer = getFirstNotBlankInteger(array);
                if (integer != null) {
                    return integer;
                }
            } else if (obj instanceof Integer) {
                return (Integer) obj;
            } else {
                String str = obj.toString();
                if (StringUtils.isNotBlank(str)) {
                    try {
                        return Integer.valueOf(str);
                    } catch (NumberFormatException e) {

                    }
                }
            }
        }
        return null;
    }

    private static String getFirstNotBlankString(JSONArray jsonArray) {
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                String str = getFirstNotBlankString(array);
                if (StringUtils.isNotBlank(str)) {
                    return str;
                }
            } else {
                String str = obj.toString();
                if (StringUtils.isNotBlank(str)) {
                    return str;
                }
            }
        }
        return null;
    }

    private static Object getFirstNotNullObject(JSONArray jsonArray) {
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                Object obj2 = getFirstNotNullObject(array);
                if (obj2 != null) {
                    return obj2;
                }
            } else {
                return obj;
            }
        }
        return null;
    }

    private static Boolean getFirstNotNullBoolean(JSONArray jsonArray) {
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                Boolean bool = getFirstNotNullBoolean(array);
                if (bool != null) {
                    return bool;
                }
            } else if (obj instanceof Boolean) {
                return (Boolean) obj;
            } else {
                String str = obj.toString();
                if (Objects.equals(str, Boolean.TRUE.toString())) {
                    return Boolean.TRUE;
                } else if (Objects.equals(str, Boolean.FALSE.toString())) {
                    return Boolean.FALSE;
                }
            }
        }
        return null;
    }

    private static Object getJSONObjectOrJSONArray(JSONArray jsonArray) {
        JSONArray jsonList = new JSONArray();
        for (Object obj : jsonArray) {
            if (obj instanceof JSONObject) {
                jsonList.add(obj);
            } else if (obj instanceof JSONArray) {
                jsonList.add(obj);
            } else if (obj instanceof Number) {
                jsonList.add(obj);
            } else {
                String str = obj.toString();
                if (str.startsWith("{") && str.endsWith("}")) {
                    JSONObject jsonObj = JSON.parseObject(str);
                    jsonList.add(jsonObj);
                } else if (str.startsWith("[") && str.endsWith("]")) {
                    JSONArray array = JSON.parseArray(str);
                    jsonList.add(array);
                } else {
                    jsonList.add(str);
                }
            }
        }
        if (jsonList.size() == 1) {
            Object obj = jsonList.get(0);
            if (obj instanceof JSONObject) {
                return obj;
            }
        }
        return jsonList;
    }

    private static JSONObject getFileInfo(JSONArray jsonArray) {
        JSONObject resultObj = new JSONObject();
        JSONArray fileIdList = new JSONArray();
        JSONArray fileList = new JSONArray();
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                JSONArray fileIdArray = jsonObj.getJSONArray("fileIdList");
                if (CollectionUtils.isNotEmpty(fileIdArray)) {
                    fileIdList.addAll(fileIdArray);
                }
                JSONArray fileArray = jsonObj.getJSONArray("fileList");
                if (CollectionUtils.isNotEmpty(fileArray)) {
                    fileList.addAll(fileArray);
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                JSONObject jsonObj = getFileInfo(array);
                JSONArray fileIdArray = jsonObj.getJSONArray("fileIdList");
                if (CollectionUtils.isNotEmpty(fileIdArray)) {
                    fileIdList.addAll(fileIdArray);
                }
                JSONArray fileArray = jsonObj.getJSONArray("fileList");
                if (CollectionUtils.isNotEmpty(fileArray)) {
                    fileList.addAll(fileArray);
                }
            } else {
                String str = obj.toString();
                if (str.startsWith("{") && str.endsWith("}")) {
                    JSONObject jsonObj = JSONObject.parseObject(str);
                    JSONArray fileIdArray = jsonObj.getJSONArray("fileIdList");
                    if (CollectionUtils.isNotEmpty(fileIdArray)) {
                        fileIdList.addAll(fileIdArray);
                    }
                    JSONArray fileArray = jsonObj.getJSONArray("fileList");
                    if (CollectionUtils.isNotEmpty(fileArray)) {
                        fileList.addAll(fileArray);
                    }
                } else if (str.startsWith("[") && str.endsWith("]")) {
                    JSONArray array = JSONArray.parseArray(str);
                    JSONObject jsonObj = getFileInfo(array);
                    JSONArray fileIdArray = jsonObj.getJSONArray("fileIdList");
                    if (CollectionUtils.isNotEmpty(fileIdArray)) {
                        fileIdList.addAll(fileIdArray);
                    }
                    JSONArray fileArray = jsonObj.getJSONArray("fileList");
                    if (CollectionUtils.isNotEmpty(fileArray)) {
                        fileList.addAll(fileArray);
                    }
                }
            }
        }
        resultObj.put("fileIdList", fileIdList);
        resultObj.put("fileList", fileList);
        return resultObj;
    }

    private static List<String> getUserSelectInfo(JSONArray jsonArray) {
        List<String> resultList = new ArrayList<>();
        for (Object obj : jsonArray) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                for (Object obj2 : array) {
                    String str = obj2.toString();
                    if (str.length() == 37) {
                        if (str.startsWith(GroupSearch.USER.getValuePlugin())
                                || str.startsWith(GroupSearch.TEAM.getValuePlugin())
                                || str.startsWith(GroupSearch.ROLE.getValuePlugin())) {
                            resultList.add(str);
                        }
                    }
                }
            } else {
                String str = obj.toString();
                if (str.length() == 37) {
                    if (str.startsWith(GroupSearch.USER.getValuePlugin())
                            || str.startsWith(GroupSearch.TEAM.getValuePlugin())
                            || str.startsWith(GroupSearch.ROLE.getValuePlugin())) {
                        resultList.add(str);
                    }
                }
            }
        }
        return resultList;
    }
}
