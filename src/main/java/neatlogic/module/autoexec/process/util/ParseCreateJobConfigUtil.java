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
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.Expression;
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
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ParseCreateJobConfigUtil {

    /**
     * 根据工单步骤配置信息创建AutoexecJobVo对象
     *
     * @param currentProcessTaskStepVo
     * @param createJobConfigConfigVo
     * @return
     */
    public static List<AutoexecJobVo> createAutoexecJobList(ProcessTaskStepVo currentProcessTaskStepVo, CreateJobConfigConfigVo createJobConfigConfigVo) {
        Long processTaskId = currentProcessTaskStepVo.getProcessTaskId();
        // 如果工单有表单信息，则查询出表单配置及数据
        Map<String, Object> formAttributeDataMap = new HashMap<>();
        Map<String, Object> originalFormAttributeDataMap = new HashMap<>();
        IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
        List<FormAttributeVo> formAttributeList = processTaskCrossoverService.getFormAttributeListByProcessTaskIdAngTag(processTaskId, createJobConfigConfigVo.getFormTag());
        if (CollectionUtils.isNotEmpty(formAttributeList)) {
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskIdAndTag(processTaskId, createJobConfigConfigVo.getFormTag());
            for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                originalFormAttributeDataMap.put(attributeDataVo.getAttributeUuid(), attributeDataVo.getDataObj());
                // 放入表单普通组件数据
                if (!Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLEINPUTER.getHandler())
                        && !Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMSUBASSEMBLY.getHandler())
                        && !Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLESELECTOR.getHandler())) {
                    formAttributeDataMap.put(attributeDataVo.getAttributeUuid(), attributeDataVo.getDataObj());
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
            AutoexecJobVo jobVo = createSingleAutoexecJobVo(currentProcessTaskStepVo, createJobConfigConfigVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
//            jobVo.setRunnerGroup(getRunnerGroup(jobVo.getParam(), autoexecConfig));
            List<AutoexecJobVo> resultList = new ArrayList<>();
            resultList.add(jobVo);
            return resultList;
        } else if (Objects.equals(createPolicy, "batch")) {
            List<AutoexecJobVo> jobVoList = createBatchAutoexecJobVo(currentProcessTaskStepVo, createJobConfigConfigVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
//            if (CollectionUtils.isNotEmpty(jobVoList)) {
//                jobVoList.forEach(jobVo -> jobVo.setRunnerGroup(getRunnerGroup(jobVo.getParam(), autoexecConfig)));
//            }
            return jobVoList;
        } else {
            return null;
        }
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
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam) {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        // 组合工具ID
        Long combopId = createJobConfigConfigVo.getCombopId();
        // 作业名称
        String jobName = createJobConfigConfigVo.getJobName();
        // 场景
        List<CreateJobConfigMappingGroupVo> scenarioParamMappingList = createJobConfigConfigVo.getScenarioParamMappingGroupList();
        if (CollectionUtils.isNotEmpty(scenarioParamMappingList)) {
            Long scenarioId = getScenarioId(scenarioParamMappingList.get(0), formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
            jobVo.setScenarioId(scenarioId);
        }
        // 作业参数赋值列表
        List<CreateJobConfigMappingGroupVo> runtimeParamMappingGroupList = createJobConfigConfigVo.getJopParamMappingGroupList();
        if (CollectionUtils.isNotEmpty(runtimeParamMappingGroupList)) {
            JSONObject param = new JSONObject();
            for(CreateJobConfigMappingGroupVo mappingGroupVo : runtimeParamMappingGroupList) {
                Object value = parseRuntimeParamMapping(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                param.put(mappingGroupVo.getKey(), value);
            }
            jobVo.setParam(param);
        }
        // 目标参数赋值列表
//        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
        List<CreateJobConfigMappingGroupVo> executeParamMappingGroupList = createJobConfigConfigVo.getExecuteParamMappingGroupList();
        if (CollectionUtils.isNotEmpty(executeParamMappingGroupList)) {
            AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
//            AutoexecCombopExecuteConfigVo executeConfig = getAutoexecCombopExecuteConfig(executeParamList, formAttributeMap, processTaskFormAttributeDataMap);
            for (CreateJobConfigMappingGroupVo mappingGroupVo : executeParamMappingGroupList) {
                String key = mappingGroupVo.getKey();
                if (Objects.equals(key, "executeNodeConfig")) {
                    Object value = parseExecuteNodeConfigMapping(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                } else if (Objects.equals(key, "protocolId")) {
                    Object value = parseProtocolIdMapping(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                } else if (Objects.equals(key, "executeUser")) {
                    Object value = parseExecuteUserParamMapping(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                } else if (Objects.equals(key, "roundCount")) {
                    Object value = parseRoundCountMapping(mappingGroupVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                }
            }
            jobVo.setExecuteConfig(executeConfig);
        }
//        String jobNamePrefixKey = autoexecConfig.getString("jobNamePrefix");

        String jobNamePrefixMappingKey = createJobConfigConfigVo.getJobNamePrefixMappingValue();
        String jobNamePrefixValue = getJobNamePrefixValue(jobNamePrefixMappingKey, jobVo.getExecuteConfig(), jobVo.getParam());

//        CreateJobConfigMappingGroupVo runnerGroupMapping = createJobConfigConfigVo.getRunnerGroupMappingGroup();
//        ParamMappingVo runnerGroupMappingVo = parseRunnerGroupMapping(runnerGroupMapping, jobVo.getParam());
        jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
        jobVo.setRoundCount(32);
        jobVo.setOperationId(combopId);
        jobVo.setName(jobNamePrefixValue + jobName);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setInvokeId(currentProcessTaskStepVo.getId());
        jobVo.setRouteId(currentProcessTaskStepVo.getId().toString());
        jobVo.setIsFirstFire(1);
        jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
//        jobVo.setRunnerGroup(runnerGroupMappingVo);
        return jobVo;
    }


    private static Integer parseRoundCountMapping(
            CreateJobConfigMappingGroupVo mappingGroupVo,
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam
    ) {
        Integer roundCount = null;
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return null;
        }
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            String mappingMode = mappingVo.getMappingMode();
            Object value = mappingVo.getValue();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                // 映射模式为表单表格组件

            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                // 映射模式为表单普通组件
                Object obj = formAttributeDataMap.get(value);
                roundCount = Integer.getInteger(obj.toString());
            } else if (Objects.equals(mappingMode, "constant")) {
                // 映射模式为常量
                roundCount = Integer.getInteger(value.toString());
            } else if (Objects.equals(mappingMode, "runtimeparam")) {
                // 映射模式为作业参数，只读

            }
        }
        return roundCount;
    }

    private static ParamMappingVo parseExecuteUserParamMapping(
            CreateJobConfigMappingGroupVo mappingGroupVo,
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam
    ) {
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return null;
        }
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            String mappingMode = mappingVo.getMappingMode();
            Object value = mappingVo.getValue();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                // 映射模式为表单表格组件

            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                // 映射模式为表单普通组件
                Object obj = formAttributeDataMap.get(value);
                if (obj != null) {
                    ParamMappingVo paramMappingVo = new ParamMappingVo();
                    paramMappingVo.setMappingMode("constant");
                    paramMappingVo.setValue(obj);
                    return paramMappingVo;
                }
            } else if (Objects.equals(mappingMode, "constant")) {
                // 映射模式为常量
                ParamMappingVo paramMappingVo = new ParamMappingVo();
                paramMappingVo.setMappingMode("constant");
                paramMappingVo.setValue(value);
                return paramMappingVo;
            } else if (Objects.equals(mappingMode, "runtimeparam")) {
                // 映射模式为作业参数，只读
                ParamMappingVo paramMappingVo = new ParamMappingVo();
                paramMappingVo.setMappingMode("runtimeparam");
                paramMappingVo.setValue(value);
                return paramMappingVo;
            }
        }
        return null;
    }

    private static Long parseProtocolIdMapping(
            CreateJobConfigMappingGroupVo mappingGroupVo,
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam
    ) {
        Long protocolId = null;
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return protocolId;
        }
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            String mappingMode = mappingVo.getMappingMode();
            Object value = mappingVo.getValue();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                // 映射模式为表单表格组件

            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                // 映射模式为表单普通组件
                Object obj = formAttributeDataMap.get(value);
                try {
                    protocolId = Long.valueOf(obj.toString());
                } catch (NumberFormatException ex) {
                    IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
                    AccountProtocolVo protocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolName(obj.toString());
                    if (protocolVo != null) {
                        protocolId = protocolVo.getId();
                    }
                }
            } else if (Objects.equals(mappingMode, "constant")) {
                // 映射模式为常量
                protocolId = Long.valueOf(value.toString());
            } else if (Objects.equals(mappingMode, "runtimeparam")) {
                // 映射模式为作业参数，只读

            }
        }
        return protocolId;
    }

    private static AutoexecCombopExecuteNodeConfigVo parseExecuteNodeConfigMapping(
            CreateJobConfigMappingGroupVo mappingGroupVo,
            List<FormAttributeVo> formAttributeList,
            Map<String, Object> originalFormAttributeDataMap,
            Map<String, Object> formAttributeDataMap,
            JSONObject processTaskParam
    ) {
        List<String> formTextAttributeList = new ArrayList<>();
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXT.getHandler());
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXTAREA.getHandler());
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return null;
        }
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            String mappingMode = mappingVo.getMappingMode();
            Object value = mappingVo.getValue();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                // 映射模式为表单表格组件
                JSONArray array = parseFormTableComponentMappingMode(mappingVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
                if (CollectionUtils.isNotEmpty(array)) {
                    AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
                    List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
                    for (int i = 0; i < array.size(); i++) {
                        String str = array.getString(i);
                        inputNodeList.add(new AutoexecNodeVo(str));
                    }
                    executeNodeConfigVo.setInputNodeList(inputNodeList);
                    return executeNodeConfigVo;
                }
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                // 映射模式为表单普通组件
                Object dataObj = formAttributeDataMap.get(value);
                if (dataObj != null) {
                    AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
                    String handler = null;
                    Optional<FormAttributeVo> first = formAttributeList.stream().filter(e -> Objects.equals(e.getUuid(), value)).findFirst();
                    if (first.isPresent()) {
                        FormAttributeVo formAttributeVo = first.get();
                        handler = formAttributeVo.getHandler();
                    }
                    if (Objects.equals(handler, FormHandler.FORMRESOURECES.getHandler())) {
                        // 映射的表单组件是执行目标
                        executeNodeConfigVo = ((JSONObject) dataObj).toJavaObject(AutoexecCombopExecuteNodeConfigVo.class);
                    } else if (formTextAttributeList.contains(handler)) {
                        // 映射的表单组件是文本框
                        String dataStr = dataObj.toString();
                        try {
                            List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
                            JSONArray array = JSONArray.parseArray(dataStr);
                            for (int j = 0; j < array.size(); j++) {
                                String str = array.getString(j);
                                inputNodeList.add(new AutoexecNodeVo(str));
                            }
                            executeNodeConfigVo.setInputNodeList(inputNodeList);
                        } catch (JSONException e) {
                            List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
                            inputNodeList.add(new AutoexecNodeVo(dataObj.toString()));
                            executeNodeConfigVo.setInputNodeList(inputNodeList);
                        }
                    } else {
                        // 映射的表单组件不是执行目标
                        List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
                        inputNodeList.add(new AutoexecNodeVo(dataObj.toString()));
                        executeNodeConfigVo.setInputNodeList(inputNodeList);
                    }
                    return executeNodeConfigVo;
                }
            } else if (Objects.equals(mappingMode, "constant")) {
                // 映射模式为常量
//            value;
            } else if (Objects.equals(mappingMode, "runtimeparam")) {
                // 映射模式为作业参数，只读
//            if (Objects.equals(key, "executeUser")) {
//                ParamMappingVo paramMappingVo = new ParamMappingVo();
//                paramMappingVo.setMappingMode("runtimeparam");
//                paramMappingVo.setValue(value);
//                executeConfig.put(key, paramMappingVo);
//            }
            }
        }

        return null;
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
            AutoexecJobVo jobVo = createSingleAutoexecJobVo(currentProcessTaskStepVo, createJobConfigConfigVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
            resultList.add(jobVo);
        }
        return resultList;
    }

    /**
     * 获取场景ID
     *
     * @param mappingGroupVo
     * @param formAttributeList
     * @param originalFormAttributeDataMap
     * @param formAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private static Long getScenarioId(CreateJobConfigMappingGroupVo mappingGroupVo,
                               List<FormAttributeVo> formAttributeList,
                               Map<String, Object> originalFormAttributeDataMap,
                               Map<String, Object> formAttributeDataMap,
                               JSONObject processTaskParam) {

        String key = mappingGroupVo.getKey();
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String type = mappingGroupVo.getType();
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return null;
        }
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            Object value = mappingVo.getValue();
            if (value == null) {
                return null;
            }
            Object scenario = null;
            String mappingMode = mappingVo.getMappingMode();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                scenario = parseFormTableComponentMappingMode(mappingVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam);
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                scenario = formAttributeDataMap.get(value);
            } else if (Objects.equals(mappingMode, "constant")) {
                scenario = value;
            }
            if (scenario != null) {
                if (scenario instanceof List) {
                    List scenarioList = (List) scenario;
                    if (CollectionUtils.isNotEmpty(scenarioList)) {
                        scenario = scenarioList.get(0);
                    }
                }
                IAutoexecScenarioCrossoverMapper autoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
                if (scenario instanceof String) {
                    String scenarioName = (String) scenario;
                    AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioByName(scenarioName);
                    if (scenarioVo != null) {
                        return scenarioVo.getId();
                    } else {
                        try {
                            Long scenarioId = Long.valueOf(scenarioName);
                            if (autoexecScenarioCrossoverMapper.getScenarioById(scenarioId) != null) {
                                return scenarioId;
                            }
                        } catch (NumberFormatException ignored) {

                        }
                    }
                } else if (scenario instanceof Long) {
                    Long scenarioId = (Long) scenario;
                    if (autoexecScenarioCrossoverMapper.getScenarioById(scenarioId) != null) {
                        return scenarioId;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 根据设置找到作业名称前缀值
     *
     * @param jobNamePrefixKey 作业名称前缀key
     * @param executeConfig    目标参数
     * @param param            作业参数
     * @return 返回作业名称前缀值
     */
    private static String getJobNamePrefixValue2(String jobNamePrefixKey, AutoexecCombopExecuteConfigVo executeConfig, JSONObject param) {
        String jobNamePrefixValue = StringUtils.EMPTY;
        if (StringUtils.isBlank(jobNamePrefixKey)) {
            return jobNamePrefixValue;
        }
        if (Objects.equals(jobNamePrefixKey, "executeNodeConfig")) {
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
        } else if (Objects.equals(jobNamePrefixKey, "executeUser")) {
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
        } else if (Objects.equals(jobNamePrefixKey, "protocolId")) {
            Long protocolId = executeConfig.getProtocolId();
            if (protocolId != null) {
                jobNamePrefixValue = protocolId.toString();
            }
        } else if (Objects.equals(jobNamePrefixKey, "roundCount")) {
            Integer roundCount = executeConfig.getRoundCount();
            if (roundCount != null) {
                jobNamePrefixValue = roundCount.toString();
            }
        } else {
            Object jobNamePrefixObj = param.get(jobNamePrefixKey);
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

    /**
     * 根据设置找到作业名称前缀值
     *
     * @param jobNamePrefixKey 作业名称前缀key
     * @param executeConfig    目标参数
     * @param param            作业参数
     * @return 返回作业名称前缀值
     */
    private static String getJobNamePrefixValue(String jobNamePrefixKey, AutoexecCombopExecuteConfigVo executeConfig, JSONObject param) {
        String jobNamePrefixValue = StringUtils.EMPTY;
        if (StringUtils.isBlank(jobNamePrefixKey)) {
            return jobNamePrefixValue;
        }
        if (Objects.equals(jobNamePrefixKey, "executeNodeConfig")) {
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
        } else if (Objects.equals(jobNamePrefixKey, "executeUser")) {
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
        } else if (Objects.equals(jobNamePrefixKey, "protocolId")) {
            Long protocolId = executeConfig.getProtocolId();
            if (protocolId != null) {
                jobNamePrefixValue = protocolId.toString();
            }
        } else if (Objects.equals(jobNamePrefixKey, "roundCount")) {
            Integer roundCount = executeConfig.getRoundCount();
            if (roundCount != null) {
                jobNamePrefixValue = roundCount.toString();
            }
        } else {
            Object jobNamePrefixObj = param.get(jobNamePrefixKey);
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

    private static Object parseRuntimeParamMapping(CreateJobConfigMappingGroupVo mappingGroupVo,
                                            List<FormAttributeVo> formAttributeList,
                                            Map<String, Object> originalFormAttributeDataMap,
                                            Map<String, Object> formAttributeDataMap,
                                            JSONObject processTaskParam) {
        List<String> formSelectAttributeList = new ArrayList<>();
        formSelectAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMSELECT.getHandler());
        formSelectAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMCHECKBOX.getHandler());
        formSelectAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMRADIO.getHandler());
        List<String> formTextAttributeList = new ArrayList<>();
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXT.getHandler());
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXTAREA.getHandler());
        String key = mappingGroupVo.getKey();
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String type = mappingGroupVo.getType();
        List<CreateJobConfigMappingVo> mappingList = mappingGroupVo.getMappingList();
        if (CollectionUtils.isEmpty(mappingList)) {
            return null;
        }
        JSONArray resultList = new JSONArray();
        for (CreateJobConfigMappingVo mappingVo : mappingList) {
            Object value = mappingVo.getValue();
            if (value == null) {
                continue;
            }
            String mappingMode = mappingVo.getMappingMode();
            if (Objects.equals(mappingMode, "formTableComponent")) {
                resultList.add(parseFormTableComponentMappingMode(mappingVo, formAttributeList, originalFormAttributeDataMap, formAttributeDataMap, processTaskParam));
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                Object obj = formAttributeDataMap.get(value);
                if (obj != null) {
                    String handler = null;
                    FormAttributeVo formAttributeVo = getFormAttributeVo(formAttributeList, value.toString());
                    if (formAttributeVo != null) {
                        handler = formAttributeVo.getHandler();
                    }
                    if (formTextAttributeList.contains(handler)) {
                        resultList.add(convertDateType(type, (String) obj));
                    } else if (formSelectAttributeList.contains(handler)) {
                        if (obj instanceof String) {
                            resultList.add(convertDateType(type, (String) obj));
                        } else if (obj instanceof JSONArray) {
                            resultList.add(convertDateType(type, JSONObject.toJSONString(obj)));
                        }
                    }
                }
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
        if (mappingList.size() == 1) {
            return resultList.get(0);
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
                if (!derivedTableDataList.contains(newRowData)) {// TODO 这里需要验证HashMap的equals方法
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
            JSONObject newJsonObj = array.getJSONObject(i);
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
                }
                newJsonObj.put(key, value);
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
     * @param paramType  作业参数类型
     * @param sourceList 某列数据集合
     * @return
     */
    private static Object convertDateType(String paramType, List<String> sourceList) {
        if (Objects.equals(paramType, ParamType.NODE.getValue())) {
            if (CollectionUtils.isNotEmpty(sourceList)) {
                JSONArray inputNodeList = new JSONArray();
                for (String str : sourceList) {
                    if (StringUtils.isNotBlank(str)) {
                        JSONArray inputNodeArray = (JSONArray) convertDateType(paramType, str);
                        inputNodeList.addAll(inputNodeArray);
                    }
                }
                return inputNodeList;
            }
        }
        return sourceList;
    }

    /**
     * 把表单文本框组件数据转换成作业参数对应的数据
     *
     * @param paramType 作业参数类型
     * @param source    数据
     * @return Object
     */
    private static Object convertDateType(String paramType, String source) {
        if (Objects.equals(paramType, ParamType.NODE.getValue())) {
            if (StringUtils.isNotBlank(source)) {
                JSONArray inputNodeList = new JSONArray();
                if (source.startsWith("[") && source.endsWith("]")) {
                    JSONArray array = JSON.parseArray(source);
                    for (int i = 0; i < array.size(); i++) {
                        String str = array.getString(i);
                        inputNodeList.add(new AutoexecNodeVo(str));
                    }
                } else if (source.contains("\n")) {
                    String[] split = source.split("\n");
                    for (String str : split) {
                        inputNodeList.add(new AutoexecNodeVo(str));
                    }
                } else {
                    inputNodeList.add(new AutoexecNodeVo(source));
                }
                return inputNodeList;
            }
        }
        return source;
    }
}
