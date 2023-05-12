/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.stephandler.component;

import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobEnvVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.process.constvalue.*;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskFormVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import neatlogic.framework.process.stephandler.core.IProcessStepHandler;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.process.stephandler.core.ProcessStepThread;
import neatlogic.module.autoexec.constvalue.FailPolicy;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/9/2 14:22
 **/
@Service
public class AutoexecProcessComponent extends ProcessStepHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(AutoexecProcessComponent.class);
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    private AutoexecJobService autoexecJobService;

    @Override
    public String getHandler() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getHandler();
    }

    @Override
    public JSONObject getChartConfig() {
        return new JSONObject() {
            {
                this.put("icon", "tsfont-zidonghua");
                this.put("shape", "L-rectangle:R-rectangle");
                this.put("width", 68);
                this.put("height", 40);
            }
        };
    }

    @Override
    public String getType() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getType();
    }

    @Override
    public ProcessStepMode getMode() {
        return ProcessStepMode.MT;
    }

    @Override
    public String getName() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getName();
    }

    @Override
    public int getSort() {
        return 10;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Boolean isAllowStart() {
        return false;
    }

    @Override
    protected int myActive(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        String configHash = currentProcessTaskStepVo.getConfigHash();
        if (StringUtils.isBlank(configHash)) {
            ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
            configHash = processTaskStepVo.getConfigHash();
            currentProcessTaskStepVo.setProcessStepUuid(processTaskStepVo.getProcessStepUuid());
        }
        String config = selectContentByHashMapper.getProcessTaskStepConfigByHash(configHash);
//        if (StringUtils.isNotBlank(config)) {
//            JSONObject autoexecConfig = (JSONObject) JSONPath.read(config, "autoexecConfig");
//            if (MapUtils.isNotEmpty(autoexecConfig)) {
//                AutoexecJobVo jobVo = createAutoexecJobVo(currentProcessTaskStepVo, autoexecConfig);
//                Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
//                //如果工单步骤ID没有绑定自动化作业ID，则需要创建自动化作业
//                if (autoexecJobId == null) {
//                    try {
//                        autoexecJobActionService.validateCreateJob(jobVo);
//                    } catch (Exception e) {
//                        logger.error(e.getMessage(), e);
//                        String failPolicy = autoexecConfig.getString("failPolicy");
//                        if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
//                            processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
//                        }
//                    }
//                } else {//否则重新刷新作业运行参数，以及节点后，需人工干预重跑作业
//                    autoexecJobService.refreshJobParam(autoexecJobId, jobVo.getParam());
//                    autoexecJobService.refreshJobNodeList(autoexecJobId, jobVo.getExecuteConfig());
//                }
//            }
//        }
        if (StringUtils.isBlank(config)) {
            return 0;
        }
        JSONObject autoexecConfig = (JSONObject) JSONPath.read(config, "autoexecConfig");
        if (MapUtils.isEmpty(autoexecConfig)) {
            return 0;
        }
        Integer rerunStepToCreateNewJob = autoexecConfig.getInteger("rerunStepToCreateNewJob");
        if (!Objects.equals(rerunStepToCreateNewJob, 1)) {
            Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
            if (autoexecJobId != null) {
                return 1;
            }
        }
        JSONArray configList = autoexecConfig.getJSONArray("configList");
        if (CollectionUtils.isEmpty(configList)) {
            return 0;
        }
        boolean flag = false;
        for (int i = 0; i < configList.size(); i++) {
            JSONObject configObj = configList.getJSONObject(i);
            if (MapUtils.isEmpty(configObj)) {
                continue;
            }
            List<AutoexecJobVo> autoexecJobList = createAutoexecJobList(currentProcessTaskStepVo, configObj);
            if (CollectionUtils.isEmpty(autoexecJobList)) {
                continue;
            }
            for (AutoexecJobVo jobVo : autoexecJobList) {
                try {
                    autoexecJobActionService.validateCreateJob(jobVo);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    flag = true;
                }
            }
        }
        if (flag) {
            String failPolicy = autoexecConfig.getString("failPolicy");
            if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
            }
        }
        return 1;
    }

    private List<AutoexecJobVo> createAutoexecJobList(ProcessTaskStepVo currentProcessTaskStepVo, JSONObject autoexecConfig) {
        Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap = new HashMap<>();
        Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
        Long processTaskId = currentProcessTaskStepVo.getProcessTaskId();
        ProcessTaskFormVo processTaskFormVo = processTaskMapper.getProcessTaskFormByProcessTaskId(processTaskId);
        if (processTaskFormVo != null) {
            String formContent = selectContentByHashMapper.getProcessTaskFromContentByHash(processTaskFormVo.getFormContentHash());
            FormVersionVo formVersionVo = new FormVersionVo();
            formVersionVo.setFormUuid(processTaskFormVo.getFormUuid());
            formVersionVo.setFormName(processTaskFormVo.getFormName());
            formVersionVo.setFormConfig(JSONObject.parseObject(formContent));
            List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
            if (CollectionUtils.isNotEmpty(formAttributeList)) {
                formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
            }
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(processTaskId);
            if (CollectionUtils.isNotEmpty(processTaskFormAttributeDataList)) {
                processTaskFormAttributeDataMap = processTaskFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
            }
        }
        String createJobPolicy = autoexecConfig.getString("createJobPolicy");
        if (Objects.equals(createJobPolicy, "single")) {
            AutoexecJobVo jobVo = createSingleAutoexecJobVo(currentProcessTaskStepVo, autoexecConfig, formAttributeMap, processTaskFormAttributeDataMap);
            if (jobVo != null) {
                List<AutoexecJobVo> resultList = new ArrayList<>();
                resultList.add(jobVo);
                return resultList;
            }
            return null;
        } else if (Objects.equals(createJobPolicy, "batch")) {
            return createBatchAutoexecJobVo(currentProcessTaskStepVo, autoexecConfig, formAttributeMap, processTaskFormAttributeDataMap);
        } else {
            return null;
        }
    }

    private AutoexecJobVo createSingleAutoexecJobVo(
            ProcessTaskStepVo currentProcessTaskStepVo,
            JSONObject autoexecConfig,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        Long combopId = autoexecConfig.getLong("autoexecCombopId");
        String jobName = autoexecConfig.getString("jobName");
        JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
        JSONObject param = new JSONObject();
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            for (int i = 0; i < runtimeParamList.size(); i++) {
                JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
                if (MapUtils.isEmpty(runtimeParamObj)) {
                    continue;
                }
                String key = runtimeParamObj.getString("key");
                if (StringUtils.isBlank(key)) {
                    continue;
                }
                Object value = runtimeParamObj.get("value");
                if (value == null) {
                    continue;
                }
                String mappingMode = runtimeParamObj.getString("mappingMode");
                if (Objects.equals(mappingMode, "formTableComponent")) {
                    String column = runtimeParamObj.getString("column");
                    FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    param.put(key, parseFormTableComponentMappingValue(formAttributeVo, attributeDataVo, column));
                } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                    param.put(key, processTaskFormAttributeDataMap.get(value));
                } else if (Objects.equals(mappingMode, "constant")) {
                    param.put(key, value);
                }
            }
            jobVo.setParam(param);
        }
        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
        JSONObject executeConfig = new JSONObject();
        if (CollectionUtils.isNotEmpty(executeParamList)) {
            for (int i = 0; i < executeParamList.size(); i++) {
                JSONObject executeParamObj = executeParamList.getJSONObject(i);
                if (MapUtils.isEmpty(executeParamObj)) {
                    continue;
                }
                String key = executeParamObj.getString("key");
                if (StringUtils.isBlank(key)) {
                    continue;
                }
                Object value = executeParamObj.get("value");
                if (value == null) {
                    continue;
                }
                String mappingMode = executeParamObj.getString("mappingMode");
                if (Objects.equals(mappingMode, "formTableComponent")) {
                    String column = executeParamObj.getString("column");
                    FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    if (Objects.equals(key, "executeUser")) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        paramMappingVo.setMappingMode("constant");
                        paramMappingVo.setValue(parseFormTableComponentMappingValue(formAttributeVo, attributeDataVo, column));
                        executeConfig.put(key, paramMappingVo);
                    } else {
                        executeConfig.put(key, parseFormTableComponentMappingValue(formAttributeVo, attributeDataVo, column));
                    }
                } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                    if (Objects.equals(key, "executeUser")) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        paramMappingVo.setMappingMode("constant");
                        ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                        if (attributeDataVo != null) {
                            paramMappingVo.setValue(attributeDataVo.getDataObj());
                        }
                        executeConfig.put(key, paramMappingVo);
                    } else {
                        ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                        if (attributeDataVo != null) {
                            executeConfig.put(key, attributeDataVo.getDataObj());
                        }
                    }
                } else if (Objects.equals(mappingMode, "constant")) {
                    if (Objects.equals(key, "executeUser")) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        paramMappingVo.setMappingMode("constant");
                        paramMappingVo.setValue(value);
                        executeConfig.put(key, paramMappingVo);
                    } else {
                        executeConfig.put(key, value);
                    }
                } else if (Objects.equals(mappingMode, "runtimeparam")) {
                    if (Objects.equals(key, "executeUser")) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        paramMappingVo.setMappingMode("runtimeparam");
                        paramMappingVo.setValue(value);
                        executeConfig.put(key, paramMappingVo);
                    }
                }
            }
            jobVo.setExecuteConfig(executeConfig.toJavaObject(AutoexecCombopExecuteConfigVo.class));
        }
        jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
        jobVo.setRoundCount(32);
        jobVo.setOperationId(combopId);
        jobVo.setName(jobName);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setInvokeId(currentProcessTaskStepVo.getId());
        jobVo.setRouteId(currentProcessTaskStepVo.getProcessStepUuid());
        jobVo.setIsFirstFire(1);
        jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
        return jobVo;
    }

    private List<AutoexecJobVo> createBatchAutoexecJobVo(
            ProcessTaskStepVo currentProcessTaskStepVo,
            JSONObject autoexecConfig,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        Long combopId = autoexecConfig.getLong("autoexecCombopId");
        String jobName = autoexecConfig.getString("jobName");
        List<AutoexecJobVo> resultList = new ArrayList<>();
        JSONObject batchJobDataSource = autoexecConfig.getJSONObject("batchJobDataSource");
        if (MapUtils.isEmpty(batchJobDataSource)) {
            return resultList;
        }
        String attributeUuid = batchJobDataSource.getString("attributeUuid");
        if (StringUtils.isBlank(attributeUuid)) {
            return resultList;
        }
        ProcessTaskFormAttributeDataVo formAttributeDataVo = processTaskFormAttributeDataMap.get(attributeUuid);
        if (formAttributeDataVo == null) {
            return resultList;
        }
        Object dataObj = formAttributeDataVo.getDataObj();
        if (dataObj == null) {
            return resultList;
        }
        JSONArray tbodyList = new JSONArray();
        JSONArray dataList = (JSONArray) dataObj;
        JSONArray filterList = batchJobDataSource.getJSONArray("filterList");
        if (CollectionUtils.isNotEmpty(filterList)) {
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject data = dataList.getJSONObject(i);
                if (MapUtils.isEmpty(data)) {
                    continue;
                }
                boolean flag = true;
                for (int j = 0; j < filterList.size(); j++) {
                    JSONObject filterObj = filterList.getJSONObject(j);
                    if (MapUtils.isEmpty(filterObj)) {
                        continue;
                    }
                    String column = filterObj.getString("column");
                    if (StringUtils.isBlank(column)) {
                        continue;
                    }
                    String expression = filterObj.getString("expression");
                    if (StringUtils.isBlank(expression)) {
                        continue;
                    }
                    String value = filterObj.getString("value");
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    if (Objects.equals(expression, Expression.EQUAL.getExpression())) {
                        if (!Objects.equals(value, data.getString(column))) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.UNEQUAL.getExpression())) {
                        if (Objects.equals(value, data.getString(column))) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.INCLUDE.getExpression())) {
                        String columnValue = data.getString(column);
                        if (StringUtils.isBlank(columnValue)) {
                            flag = false;
                            break;
                        }
                        if (!columnValue.contains(value)) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.EXCLUDE.getExpression())) {
                        String columnValue = data.getString(column);
                        if (StringUtils.isBlank(columnValue)) {
                            continue;
                        }
                        if (columnValue.contains(value)) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    tbodyList.add(data);
                }
            }
        } else {
            tbodyList = dataList;
        }
        if (CollectionUtils.isEmpty(tbodyList)) {
            return resultList;
        }
        for (int index = 0; index < tbodyList.size(); index++) {
            AutoexecJobVo jobVo = new AutoexecJobVo();
            jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
            jobVo.setRoundCount(32);
            jobVo.setOperationId(combopId);
            jobVo.setName(jobName);
            jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
            jobVo.setInvokeId(currentProcessTaskStepVo.getId());
            jobVo.setRouteId(currentProcessTaskStepVo.getProcessStepUuid());
            jobVo.setIsFirstFire(1);
            jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
            JSONObject tbodyObj = tbodyList.getJSONObject(index);
            JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
            if (CollectionUtils.isNotEmpty(executeParamList)) {
                JSONObject executeConfig = new JSONObject();
                for (int i = 0; i < executeParamList.size(); i++) {
                    JSONObject executeParamObj = executeParamList.getJSONObject(i);
                    if (MapUtils.isEmpty(executeParamObj)) {
                        continue;
                    }
                    String key = executeParamObj.getString("key");
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    String mappingMode = executeParamObj.getString("mappingMode");
                    Object value = executeParamObj.get("value");
                    if (Objects.equals(mappingMode, "formTableComponent")) {
                        String column = executeParamObj.getString("column");
                        if (Objects.equals(value, attributeUuid)) {
                            String columnValue = tbodyObj.getString(column);
                            executeConfig.put(key, columnValue);
                        } else {
                            FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                            ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                            if (Objects.equals(key, "executeUser")) {
                                ParamMappingVo paramMappingVo = new ParamMappingVo();
                                paramMappingVo.setMappingMode("constant");
                                paramMappingVo.setValue(parseFormTableComponentMappingValue(formAttributeVo, attributeDataVo, column));
                                executeConfig.put(key, paramMappingVo);
                            } else {
                                executeConfig.put(key, parseFormTableComponentMappingValue(formAttributeVo, attributeDataVo, column));
                            }
                        }
                    } else {
                        if (Objects.equals(mappingMode, "formCommonComponent")) {
                            if (Objects.equals(key, "executeUser")) {
                                ParamMappingVo paramMappingVo = new ParamMappingVo();
                                paramMappingVo.setMappingMode("constant");
                                ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                                if (attributeDataVo != null) {
                                    paramMappingVo.setValue(attributeDataVo.getDataObj());
                                }
                                executeConfig.put(key, paramMappingVo);
                            } else {
                                ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                                if (attributeDataVo != null) {
                                    executeConfig.put(key, attributeDataVo.getDataObj());
                                }
                            }
                        } else if (Objects.equals(mappingMode, "constant")) {
                            if (Objects.equals(key, "executeUser")) {
                                ParamMappingVo paramMappingVo = new ParamMappingVo();
                                paramMappingVo.setMappingMode("constant");
                                paramMappingVo.setValue(value);
                                executeConfig.put(key, paramMappingVo);
                            } else {
                                executeConfig.put(key, value);
                            }
                        } else if (Objects.equals(mappingMode, "runtimeparam")) {
                            if (Objects.equals(key, "executeUser")) {
                                ParamMappingVo paramMappingVo = new ParamMappingVo();
                                paramMappingVo.setMappingMode("runtimeparam");
                                paramMappingVo.setValue(value);
                                executeConfig.put(key, paramMappingVo);
                            }
                        }
                    }
                }
                jobVo.setExecuteConfig(executeConfig.toJavaObject(AutoexecCombopExecuteConfigVo.class));
            }
            JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
            if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                JSONObject param = new JSONObject();
                for (int i = 0; i < runtimeParamList.size(); i++) {
                    JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
                    if (MapUtils.isEmpty(runtimeParamObj)) {
                        continue;
                    }
                    String key = runtimeParamObj.getString("key");
                    if (StringUtils.isNotBlank(key)) {
                        continue;
                    }
                    Object value = runtimeParamObj.get("value");
                    if (value == null) {
                        continue;
                    }
                    String mappingMode = runtimeParamObj.getString("mappingMode");
                    if (Objects.equals(mappingMode, "formTableComponent")) {
                        String column = runtimeParamObj.getString("column");
                        if (Objects.equals(value, attributeUuid)) {
                            String columnValue = tbodyObj.getString(column);
                            param.put(key, columnValue);
                        } else {
                            FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                            ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                            param.put(key, parseFormTableComponentMappingValue(formAttributeVo, attributeDataVo, column));
                        }
                    } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                        ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                        if (attributeDataVo != null) {
                            param.put(key, attributeDataVo.getDataObj());
                        }
                    } else if (Objects.equals(mappingMode, "constant")) {
                        param.put(key, value);
                    }
                }
                jobVo.setParam(param);
            }
            resultList.add(jobVo);
        }
        return resultList;
    }

    private List<String> parseFormTableComponentMappingValue(FormAttributeVo formAttributeVo, ProcessTaskFormAttributeDataVo formAttributeDataVo, String column) {
        List<String> resultList = new ArrayList<>();
        JSONArray tbodyList = (JSONArray) formAttributeDataVo.getDataObj();
        if (CollectionUtils.isEmpty(tbodyList)) {
            return resultList;
        }
        for (int i = 0; i < tbodyList.size(); i++) {
            JSONObject tbodyObj = tbodyList.getJSONObject(i);
            if (MapUtils.isEmpty(tbodyObj)) {
                continue;
            }
            String columnValue = tbodyObj.getString(column);
            if (StringUtils.isBlank(columnValue)) {
                continue;
            }
            resultList.add(columnValue);
        }
        return resultList;
    }

    private AutoexecJobVo createAutoexecJobVo(ProcessTaskStepVo currentProcessTaskStepVo, JSONObject autoexecConfig) {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        Long combopId = autoexecConfig.getLong("autoexecCombopId");
        jobVo.setCombopId(combopId);
        JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
        JSONObject param = new JSONObject();
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            for (int i = 0; i < runtimeParamList.size(); i++) {
                JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(runtimeParamObj)) {
                    String key = runtimeParamObj.getString("key");
                    if (StringUtils.isNotBlank(key)) {
                        Object value = runtimeParamObj.get("value");
                        if (value != null) {
                            String mappingMode = runtimeParamObj.getString("mappingMode");
                            param.put(key, parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                        } else {
                            param.put(key, value);
                        }
                    }
                }
            }
            jobVo.setParam(param);
        }
        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
        JSONObject executeConfig = new JSONObject();
        if (CollectionUtils.isNotEmpty(executeParamList)) {
            for (int i = 0; i < executeParamList.size(); i++) {
                JSONObject executeParamObj = executeParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(executeParamObj)) {
                    String key = executeParamObj.getString("key");
                    Object value = executeParamObj.get("value");
                    String mappingMode = executeParamObj.getString("mappingMode");
                    if (Objects.equals(key, "executeUser")) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        if (Objects.equals(mappingMode, "runtimeparam") || Objects.equals(mappingMode, "constant")) {
                            paramMappingVo.setMappingMode(mappingMode);
                            paramMappingVo.setValue(value);
                        } else {
                            paramMappingVo.setMappingMode("constant");
                            paramMappingVo.setValue(parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                        }
                        executeConfig.put(key, paramMappingVo);
                    } else {
                        executeConfig.put(key, parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                    }
                }
            }
            jobVo.setExecuteConfig(executeConfig.toJavaObject(AutoexecCombopExecuteConfigVo.class));
        }
        jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
        jobVo.setRoundCount(32);
        jobVo.setOperationId(combopId);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setInvokeId(currentProcessTaskStepVo.getId());
        jobVo.setRouteId(currentProcessTaskStepVo.getProcessStepUuid());
        jobVo.setIsFirstFire(1);
        jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
        return jobVo;
    }

    private void processTaskStepComplete(Long processTaskStepId, Long autoexecJobId) {
        List<Long> toProcessTaskStepIdList = processTaskMapper.getToProcessTaskStepIdListByFromIdAndType(processTaskStepId, ProcessFlowDirection.FORWARD.getValue());
        if (toProcessTaskStepIdList.size() == 1) {
            Long nextStepId = toProcessTaskStepIdList.get(0);
            IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(AutoexecProcessStepHandlerType.AUTOEXEC.getHandler());
            if (handler != null) {
                try {
                    List<String> hidecomponentList = new ArrayList<>();
                    JSONArray formAttributeDataList = new JSONArray();
                    ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
                    String config = selectContentByHashMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
                    if (StringUtils.isNotBlank(config)) {
                        JSONArray formAttributeList = (JSONArray) JSONPath.read(config, "autoexecConfig.formAttributeList");
                        if (CollectionUtils.isNotEmpty(formAttributeList)) {
                            Map<String, String> autoexecJobEnvMap = new HashMap<>();
                            if (autoexecJobId != null) {
                                List<AutoexecJobEnvVo> autoexecJobEnvList = autoexecJobMapper.getAutoexecJobEnvListByJobId(autoexecJobId);
                                for (AutoexecJobEnvVo autoexecJobEnvVo : autoexecJobEnvList) {
                                    autoexecJobEnvMap.put(autoexecJobEnvVo.getName(), autoexecJobEnvVo.getValue());
                                }
                            }
                            Map<String, Object> formAttributeNewDataMap = new HashMap<>();
                            for (int i = 0; i < formAttributeList.size(); i++) {
                                JSONObject formAttributeObj = formAttributeList.getJSONObject(i);
                                String key = formAttributeObj.getString("key");
                                String value = formAttributeObj.getString("value");
                                formAttributeNewDataMap.put(key, autoexecJobEnvMap.get(value));
                            }
                            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(processTaskStepVo.getProcessTaskId());
                            for (ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo : processTaskFormAttributeDataList) {
                                JSONObject formAttributeDataObj = new JSONObject();
                                String attributeUuid = processTaskFormAttributeDataVo.getAttributeUuid();
                                formAttributeDataObj.put("attributeUuid", attributeUuid);
                                formAttributeDataObj.put("handler", processTaskFormAttributeDataVo.getType());
                                Object newData = formAttributeNewDataMap.get(attributeUuid);
                                if (newData != null) {
                                    formAttributeDataObj.put("dataList", newData);
                                } else {
                                    formAttributeDataObj.put("dataList", processTaskFormAttributeDataVo.getDataObj());
                                    hidecomponentList.add(attributeUuid);
                                }
                                formAttributeDataList.add(formAttributeDataObj);
                            }
                        }
                    }
                    JSONObject paramObj = processTaskStepVo.getParamObj();
                    paramObj.put("nextStepId", nextStepId);
                    paramObj.put("action", ProcessTaskOperationType.STEP_COMPLETE.getValue());
                    if (CollectionUtils.isNotEmpty(formAttributeDataList)) {
                        paramObj.put("formAttributeDataList", formAttributeDataList);
                    }
                    if (CollectionUtils.isNotEmpty(hidecomponentList)) {
                        paramObj.put("hidecomponentList", hidecomponentList);
                    }
                    /* 自动处理 **/
                    doNext(ProcessTaskOperationType.STEP_COMPLETE, new ProcessStepThread(processTaskStepVo) {
                        @Override
                        public void myExecute() {
                            handler.autoComplete(processTaskStepVo);
                        }
                    });
                } catch (ProcessTaskNoPermissionException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private Object parseMappingValue(ProcessTaskStepVo currentProcessTaskStepVo, String mappingMode, Object value) {
        if ("form".equals(mappingMode)) {
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataVoList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
            for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataVoList) {
                if (Objects.equals(value, attributeDataVo.getAttributeUuid())) {
                    return attributeDataVo.getDataObj();
                }
            }
            return null;
        } else if ("prestepexportparam".equals(mappingMode)) {
            return getPreStepExportParamValue(currentProcessTaskStepVo.getProcessTaskId(), (String) value);
        }
        return value;
    }

    private String getPreStepExportParamValue(Long processTaskId, String paramKey) {
        String split[] = paramKey.split("&&", 2);
        String processStepUuid = split[0];
        ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoByProcessTaskIdAndProcessStepUuid(processTaskId, processStepUuid);
        if (processTaskStepVo != null) {
            Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(processTaskStepVo.getId());
            if (autoexecJobId != null) {
                String paramName = split[1];
                AutoexecJobEnvVo autoexecJobEnvVo = new AutoexecJobEnvVo();
                autoexecJobEnvVo.setJobId(autoexecJobId);
                autoexecJobEnvVo.setName(paramName);
                return autoexecJobMapper.getAutoexecJobEnvValueByJobIdAndName(autoexecJobEnvVo);
            }
        }
        return null;
    }

    @Override
    protected int myAssign(ProcessTaskStepVo currentProcessTaskStepVo, Set<ProcessTaskStepWorkerVo> workerSet) throws ProcessTaskException {
        return defaultAssign(currentProcessTaskStepVo, workerSet);
    }

    @Override
    protected int myHang(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myHandle(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myStart(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myComplete(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myCompleteAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        if (StringUtils.isNotBlank(currentProcessTaskStepVo.getError())) {
            currentProcessTaskStepVo.getParamObj().put(ProcessTaskAuditDetailType.CAUSE.getParamName(), currentProcessTaskStepVo.getError());
        }
        /** 处理历史记录 **/
        String action = currentProcessTaskStepVo.getParamObj().getString("action");
        IProcessStepHandlerUtil.audit(currentProcessTaskStepVo, ProcessTaskAuditType.getProcessTaskAuditType(action));
        return 1;
    }

    @Override
    protected int myReapproval(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myReapprovalAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRetreat(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myAbort(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRecover(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myPause(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myTransfer(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myBack(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int mySaveDraft(ProcessTaskStepVo processTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myStartProcess(ProcessTaskStepVo processTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected Set<Long> myGetNext(ProcessTaskStepVo currentProcessTaskStepVo, List<Long> nextStepIdList, Long nextStepId) throws ProcessTaskException {
        return defaultGetNext(nextStepIdList, nextStepId);
    }

    @Override
    protected int myRedo(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }
}
