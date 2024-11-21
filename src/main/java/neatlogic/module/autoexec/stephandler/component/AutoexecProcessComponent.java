/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.stephandler.component;

import com.alibaba.fastjson.*;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobEnvVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.form.attribute.core.FormAttributeDataConversionHandlerFactory;
import neatlogic.framework.form.attribute.core.IFormAttributeDataConversionHandler;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.notify.core.INotifyParamHandler;
import neatlogic.framework.notify.core.NotifyParamHandlerFactory;
import neatlogic.framework.process.condition.core.ProcessTaskConditionFactory;
import neatlogic.framework.process.constvalue.*;
import neatlogic.framework.process.crossover.*;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import neatlogic.framework.process.notify.constvalue.ProcessTaskNotifyParam;
import neatlogic.framework.process.notify.constvalue.ProcessTaskStepNotifyParam;
import neatlogic.framework.process.notify.constvalue.ProcessTaskStepNotifyTriggerType;
import neatlogic.framework.process.stephandler.core.IProcessStepHandler;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.process.stephandler.core.ProcessStepThread;
import neatlogic.framework.util.FormUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.autoexec.constvalue.FailPolicy;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/9/2 14:22
 **/
//@Service
@Deprecated
public class AutoexecProcessComponent extends ProcessStepHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(AutoexecProcessComponent.class);

//    private final String FORM_EXTEND_ATTRIBUTE_TAG = "common";
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    private AutoexecScenarioMapper autoexecScenarioMapper;

    @Resource
    private RunnerMapper runnerMapper;

    @Resource
    private FileMapper fileMapper;


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
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
        IProcessTaskStepDataCrossoverMapper processTaskStepDataCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskStepDataCrossoverMapper.class);
        try {
            String configHash = currentProcessTaskStepVo.getConfigHash();
            if (StringUtils.isBlank(configHash)) {
                ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
                configHash = processTaskStepVo.getConfigHash();
                currentProcessTaskStepVo.setProcessStepUuid(processTaskStepVo.getProcessStepUuid());
            }
            // 获取工单当前步骤配置信息
            String config = selectContentByHashCrossoverMapper.getProcessTaskStepConfigByHash(configHash);
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
                processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                return 0;
            }
            JSONObject autoexecConfig = (JSONObject) JSONPath.read(config, "autoexecConfig");
            if (MapUtils.isEmpty(autoexecConfig)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                return 0;
            }
            // rerunStepToCreateNewJob为1时表示重新激活自动化步骤时创建新作业，rerunStepToCreateNewJob为0时表示重新激活自动化步骤时不创建新作业，也不重跑旧作业，即什么都不做
            Integer rerunStepToCreateNewJob = autoexecConfig.getInteger("rerunStepToCreateNewJob");
            if (!Objects.equals(rerunStepToCreateNewJob, 1)) {
                Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
                if (autoexecJobId != null) {
                    processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                    return 1;
                }
            }
            autoexecJobMapper.deleteAutoexecJobByProcessTaskStepId(currentProcessTaskStepVo.getId());
            // 删除上次创建作业的报错信息
            ProcessTaskStepDataVo processTaskStepData = new ProcessTaskStepDataVo();
            processTaskStepData.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
            processTaskStepData.setProcessTaskStepId(currentProcessTaskStepVo.getId());
            processTaskStepData.setType("autoexecCreateJobError");
            processTaskStepDataCrossoverMapper.deleteProcessTaskStepData(processTaskStepData);
            JSONArray configList = autoexecConfig.getJSONArray("configList");
            if (CollectionUtils.isEmpty(configList)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                return 0;
            }
            JSONArray errorMessageList = new JSONArray();
            boolean flag = false;
            List<Long> jobIdList = new ArrayList<>();
            for (int i = 0; i < configList.size(); i++) {
                JSONObject configObj = configList.getJSONObject(i);
                if (MapUtils.isEmpty(configObj)) {
                    continue;
                }
                // 根据配置信息创建AutoexecJobVo对象
                List<AutoexecJobVo> autoexecJobList = createAutoexecJobList(currentProcessTaskStepVo, configObj);
                if (CollectionUtils.isEmpty(autoexecJobList)) {
                    continue;
                }
                for (AutoexecJobVo jobVo : autoexecJobList) {
                    try {
                        autoexecJobActionService.validateCreateJob(jobVo);
                        autoexecJobMapper.insertAutoexecJobProcessTaskStep(jobVo.getId(), currentProcessTaskStepVo.getId());
                        jobIdList.add(jobVo.getId());
                    } catch (Exception e) {
                        // 增加提醒
                        logger.error(e.getMessage(), e);
                        JSONObject jobObj = new JSONObject();
                        jobObj.put("param", jobVo.getParam());
                        jobObj.put("scenarioId", jobVo.getScenarioId());
                        jobObj.put("executeConfig", jobVo.getExecuteConfig());
                        jobObj.put("runnerGroup", jobVo.getRunnerGroup());
                        jobObj.put("id", jobVo.getId());
                        jobObj.put("name", jobVo.getName());
                        jobObj.put("source", jobVo.getSource());
                        jobObj.put("roundCount", jobVo.getRoundCount());
                        jobObj.put("operationId", jobVo.getOperationId());
                        jobObj.put("operationType", jobVo.getOperationType());
                        jobObj.put("invokeId", jobVo.getInvokeId());
                        jobObj.put("routeId", jobVo.getRouteId());
                        jobObj.put("isFirstFire", jobVo.getIsFirstFire());
                        jobObj.put("assignExecUser", jobVo.getAssignExecUser());
                        logger.error(jobObj.toJSONString());
                        JSONObject errorMessageObj = new JSONObject();
                        errorMessageObj.put("jobId", jobVo.getId());
                        errorMessageObj.put("jobName", jobVo.getName());
                        errorMessageObj.put("error", e.getMessage() + " jobVo=" + jobObj.toJSONString());
                        errorMessageList.add(errorMessageObj);
                        flag = true;
                    }
                }
            }

            // 如果有一个作业创建有异常，则根据失败策略执行操作
            if (flag) {
                ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo();
                processTaskStepDataVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                processTaskStepDataVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                processTaskStepDataVo.setType("autoexecCreateJobError");
                JSONObject dataObj = new JSONObject();
                dataObj.put("errorList", errorMessageList);
                processTaskStepDataVo.setData(dataObj.toJSONString());
                processTaskStepDataVo.setFcu(UserContext.get().getUserUuid());
                processTaskStepDataCrossoverMapper.replaceProcessTaskStepData(processTaskStepDataVo);
                String failPolicy = autoexecConfig.getString("failPolicy");
                if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                    if (CollectionUtils.isNotEmpty(jobIdList)) {
                        int running = 0;
                        List<AutoexecJobVo> autoexecJobList = autoexecJobMapper.getJobListByIdList(jobIdList);
                        for (AutoexecJobVo autoexecJobVo : autoexecJobList) {
                            if (JobStatus.isRunningStatus(autoexecJobVo.getStatus())) {
                                running++;
                            }
                        }
                        if (running == 0) {
                            processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                        }
                    } else {
                        processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                    }
                } else {
                    IProcessStepHandler processStepHandler = ProcessStepHandlerFactory.getHandler(currentProcessTaskStepVo.getHandler());
                    if (processStepHandler != null) {
                        try {
                            processStepHandler.assign(currentProcessTaskStepVo);
                        } catch (ProcessTaskException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                IProcessStepHandlerCrossoverUtil processStepHandlerCrossoverUtil = CrossoverServiceFactory.getApi(IProcessStepHandlerCrossoverUtil.class);
                /* 触发通知 **/
                processStepHandlerCrossoverUtil.notify(currentProcessTaskStepVo, AutoexecNotifyTriggerType.CREATE_JOB_FAILED);
            } else if (CollectionUtils.isEmpty(jobIdList)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ProcessTaskException(e);
        }
        return 1;
    }

    /**
     * 根据工单步骤配置信息创建AutoexecJobVo对象
     *
     * @param currentProcessTaskStepVo
     * @param autoexecConfig
     * @return
     */
    private List<AutoexecJobVo> createAutoexecJobList(ProcessTaskStepVo currentProcessTaskStepVo, JSONObject autoexecConfig) {
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
        Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap = new HashMap<>();
        Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
        Long processTaskId = currentProcessTaskStepVo.getProcessTaskId();
        // 如果工单有表单信息，则查询出表单配置及数据
//        ProcessTaskFormVo processTaskFormVo = processTaskCrossoverMapper.getProcessTaskFormByProcessTaskId(processTaskId);
//        if (processTaskFormVo != null) {
//            String formContent = selectContentByHashCrossoverMapper.getProcessTaskFromContentByHash(processTaskFormVo.getFormContentHash());
//            FormVersionVo formVersionVo = new FormVersionVo();
//            formVersionVo.setFormUuid(processTaskFormVo.getFormUuid());
//            formVersionVo.setFormName(processTaskFormVo.getFormName());
//            formVersionVo.setFormConfig(JSON.parseObject(formContent));
//            List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
//            if (CollectionUtils.isNotEmpty(formAttributeList)) {
//                formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
//            }
//            IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
//            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(processTaskId);
//            if (CollectionUtils.isNotEmpty(processTaskFormAttributeDataList)) {
//                processTaskFormAttributeDataMap = processTaskFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
//            }
//        }
        String formTag = autoexecConfig.getString("formTag");
        IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
//        List<FormAttributeVo> formAttributeList = processTaskCrossoverService.getFormAttributeListByProcessTaskIdAngTag(processTaskId, FORM_EXTEND_ATTRIBUTE_TAG);
        List<FormAttributeVo> formAttributeList = processTaskCrossoverService.getFormAttributeListByProcessTaskIdAngTagNew(processTaskId, formTag);
        if (CollectionUtils.isNotEmpty(formAttributeList)) {
//            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskIdAndTag(processTaskId, FORM_EXTEND_ATTRIBUTE_TAG);
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskIdAndTagNew(processTaskId, formTag);
            // 添加表格组件中的子组件到组件列表中
            for (FormAttributeVo formAttributeVo : formAttributeList) {
                formAttributeMap.put(formAttributeVo.getUuid(), formAttributeVo);
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
                    formAttributeMap.put(downwardFormAttribute.getUuid(), downwardFormAttribute);
                }
            }
            for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                FormAttributeVo formAttributeVo = formAttributeMap.get(attributeDataVo.getAttributeUuid());
                if (formAttributeVo != null) {
                    IFormAttributeDataConversionHandler formAttributeDataConversionHandler = FormAttributeDataConversionHandlerFactory.getHandler(formAttributeVo.getHandler());
                    if (formAttributeDataConversionHandler != null) {
                        Object simpleValue = formAttributeDataConversionHandler.getSimpleValue(attributeDataVo.getDataObj());
                        attributeDataVo.setDataObj(simpleValue);
                    }
                }
            }
            processTaskFormAttributeDataMap = processTaskFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
        }

        JSONObject processTaskParam = ProcessTaskConditionFactory.getConditionParamData(Arrays.stream(ConditionProcessTaskOptions.values()).map(ConditionProcessTaskOptions::getValue).collect(Collectors.toList()), currentProcessTaskStepVo);
        // 作业策略createJobPolicy为single时表示单次创建作业，createJobPolicy为batch时表示批量创建作业
        String createJobPolicy = autoexecConfig.getString("createJobPolicy");
        if (Objects.equals(createJobPolicy, "single")) {
            AutoexecJobVo jobVo = createSingleAutoexecJobVo(currentProcessTaskStepVo, autoexecConfig, formAttributeMap, processTaskFormAttributeDataMap, processTaskParam);
            jobVo.setRunnerGroup(getRunnerGroup(jobVo.getParam(), autoexecConfig));
            List<AutoexecJobVo> resultList = new ArrayList<>();
            resultList.add(jobVo);
            return resultList;
        } else if (Objects.equals(createJobPolicy, "batch")) {
            List<AutoexecJobVo> jobVoList = createBatchAutoexecJobVo(currentProcessTaskStepVo, autoexecConfig, formAttributeMap, processTaskFormAttributeDataMap, processTaskParam);
            if (CollectionUtils.isNotEmpty(jobVoList)) {
                jobVoList.forEach(jobVo -> jobVo.setRunnerGroup(getRunnerGroup(jobVo.getParam(), autoexecConfig)));
            }
            return jobVoList;
        } else {
            return null;
        }
    }

    /**
     * 获取执行器组的值
     *
     * @param jobParamJson   作业参数的值
     * @param autoexecConfig 流程自动化节点配置
     */
    private ParamMappingVo getRunnerGroup(JSONObject jobParamJson, JSONObject autoexecConfig) {
        //执行器组
        ParamMappingVo runnerGroup = new ParamMappingVo();
        JSONObject runnerGroupJson = autoexecConfig.getJSONObject("runnerGroup");
        if (MapUtils.isNotEmpty(runnerGroupJson)) {
            String mappingMode = runnerGroupJson.getString("mappingMode");
            String mappingValue = runnerGroupJson.getString("value");
            long runnerGroupId = -1L;
            runnerGroup.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            if (Objects.equals(mappingMode, "runtimeparam")) {
                mappingValue = jobParamJson.getString(mappingValue);
                try {
                    JSONObject jsonObject = JSON.parseObject(mappingValue);
                    mappingValue = jsonObject.getString("value");
                } catch (RuntimeException ignored) {
                }
            }
            try {
                runnerGroupId = Long.parseLong(mappingValue);
                RunnerGroupVo runnerGroupVo = runnerMapper.getRunnerGroupById(runnerGroupId);
                if (runnerGroupVo == null) {
                    runnerGroupVo = runnerMapper.getRunnerGroupByName(mappingValue);
                    if (runnerGroupVo != null) {
                        runnerGroupId = runnerGroupVo.getId();
                    }
                }
            } catch (NumberFormatException ex) {
                RunnerGroupVo runnerGroupVo = runnerMapper.getRunnerGroupByName(mappingValue);
                if (runnerGroupVo != null) {
                    runnerGroupId = runnerGroupVo.getId();
                }
            }
            runnerGroup.setValue(runnerGroupId);
        }
        return runnerGroup;
    }

    /**
     * 单次创建作业
     *
     * @param currentProcessTaskStepVo
     * @param autoexecConfig
     * @param formAttributeMap
     * @param processTaskFormAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private AutoexecJobVo createSingleAutoexecJobVo(
            ProcessTaskStepVo currentProcessTaskStepVo,
            JSONObject autoexecConfig,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap,
            JSONObject processTaskParam) {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        // 组合工具ID
        Long combopId = autoexecConfig.getLong("autoexecCombopId");
        // 作业名称
        String jobName = autoexecConfig.getString("jobName");
        String jobNamePrefixKey = autoexecConfig.getString("jobNamePrefix");
        // 场景
        JSONArray scenarioParamList = autoexecConfig.getJSONArray("scenarioParamList");
        if (CollectionUtils.isNotEmpty(scenarioParamList)) {
            Long scenarioId = getScenarioId(scenarioParamList.getJSONObject(0), null, formAttributeMap, processTaskFormAttributeDataMap);
            jobVo.setScenarioId(scenarioId);
        }
        // 作业参数赋值列表
        JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            JSONObject param = getParam(currentProcessTaskStepVo, runtimeParamList, formAttributeMap, processTaskFormAttributeDataMap, processTaskParam);
            jobVo.setParam(param);
        }
        // 目标参数赋值列表
        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
        if (CollectionUtils.isNotEmpty(executeParamList)) {
            AutoexecCombopExecuteConfigVo executeConfig = getAutoexecCombopExecuteConfig(executeParamList, formAttributeMap, processTaskFormAttributeDataMap);
            jobVo.setExecuteConfig(executeConfig);
        }

        String jobNamePrefixValue = getJobNamePrefixValue(jobNamePrefixKey, jobVo.getExecuteConfig(), jobVo.getParam());
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

    /**
     * 批量创建作业
     *
     * @param currentProcessTaskStepVo
     * @param autoexecConfig
     * @param formAttributeMap
     * @param processTaskFormAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private List<AutoexecJobVo> createBatchAutoexecJobVo(
            ProcessTaskStepVo currentProcessTaskStepVo,
            JSONObject autoexecConfig,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap,
            JSONObject processTaskParam) {
        // 组合工具ID
        Long combopId = autoexecConfig.getLong("autoexecCombopId");
        // 作业名称
        String jobName = autoexecConfig.getString("jobName");
        String jobNamePrefixKey = autoexecConfig.getString("jobNamePrefix");
        List<AutoexecJobVo> resultList = new ArrayList<>();
        // 批量遍历表格
        JSONObject batchJobDataSource = autoexecConfig.getJSONObject("batchJobDataSource");
        if (MapUtils.isEmpty(batchJobDataSource)) {
            return resultList;
        }
        String attributeUuid = batchJobDataSource.getString("attributeUuid");
        ProcessTaskFormAttributeDataVo formAttributeDataVo = processTaskFormAttributeDataMap.get(attributeUuid);
        JSONArray filterList = batchJobDataSource.getJSONArray("filterList");
        JSONArray tbodyList = getTbodyList(formAttributeDataVo, filterList, formAttributeMap);
        if (CollectionUtils.isEmpty(tbodyList)) {
            return resultList;
        }
        // 遍历表格数据，创建AutoexecJobVo对象列表
        for (int index = 0; index < tbodyList.size(); index++) {
            AutoexecJobVo jobVo = new AutoexecJobVo();
            jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
            jobVo.setRoundCount(32);
            jobVo.setOperationId(combopId);
            jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
            jobVo.setInvokeId(currentProcessTaskStepVo.getId());
            jobVo.setRouteId(currentProcessTaskStepVo.getId().toString());
            jobVo.setIsFirstFire(1);
            jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
            JSONObject tbodyObj = tbodyList.getJSONObject(index);
            // 场景
            JSONArray scenarioParamList = autoexecConfig.getJSONArray("scenarioParamList");
            if (CollectionUtils.isNotEmpty(scenarioParamList)) {
                Long scenarioId = getScenarioId(scenarioParamList.getJSONObject(0), tbodyObj, formAttributeMap, processTaskFormAttributeDataMap);
                jobVo.setScenarioId(scenarioId);
            }
            // 目标参数赋值列表
            JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
            if (CollectionUtils.isNotEmpty(executeParamList)) {
                AutoexecCombopExecuteConfigVo executeConfig = getAutoexecCombopExecuteConfig(executeParamList, tbodyObj, formAttributeMap, processTaskFormAttributeDataMap);
                jobVo.setExecuteConfig(executeConfig);
            }
            // 作业参数赋值列表
            JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
            if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                JSONObject param = getParam(currentProcessTaskStepVo, runtimeParamList, tbodyObj, formAttributeMap, processTaskFormAttributeDataMap, processTaskParam);
                jobVo.setParam(param);
            }
            String jobNamePrefixValue = getJobNamePrefixValue(jobNamePrefixKey, jobVo.getExecuteConfig(), jobVo.getParam());
            jobVo.setName(jobNamePrefixValue + jobName);
            resultList.add(jobVo);
        }
        return resultList;
    }

    /**
     * 获取场景ID
     *
     * @param scenarioParamObj
     * @param tbodyObj
     * @param formAttributeMap
     * @param processTaskFormAttributeDataMap
     * @return
     */
    private Long getScenarioId(JSONObject scenarioParamObj,
                               JSONObject tbodyObj,
                               Map<String, FormAttributeVo> formAttributeMap,
                               Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        String key = scenarioParamObj.getString("key");
        if (StringUtils.isBlank(key)) {
            return null;
        }
        Object value = scenarioParamObj.get("value");
        if (value == null) {
            return null;
        }
        Object scenario = null;
        String type = scenarioParamObj.getString("type");
        String mappingMode = scenarioParamObj.getString("mappingMode");
        if (Objects.equals(mappingMode, "formTableComponent")) {
            String column = scenarioParamObj.getString("column");
            if (tbodyObj != null) {
                String columnValue = tbodyObj.getString(column);
                scenario = columnValue;
            } else {
                FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                JSONArray filterList = scenarioParamObj.getJSONArray("filterList");
                JSONArray tbodyList = getTbodyList(attributeDataVo, filterList, formAttributeMap);
                List<String> list = parseFormTableComponentMappingValue(formAttributeVo, tbodyList, column);
                scenario = convertDateType(type, list);
            }
        } else if (Objects.equals(mappingMode, "formCommonComponent")) {
            ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
            if (attributeDataVo != null) {
                List<String> formTextAttributeList = new ArrayList<>();
                formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXT.getHandler());
                formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXTAREA.getHandler());
                if (formTextAttributeList.contains(attributeDataVo.getHandler())) {
                    scenario = convertDateType(type, (String) attributeDataVo.getDataObj());
                } else {
                    scenario = attributeDataVo.getDataObj();
                }
            }
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
            if (scenario instanceof String) {
                String scenarioName = (String) scenario;
                AutoexecScenarioVo scenarioVo = autoexecScenarioMapper.getScenarioByName(scenarioName);
                if (scenarioVo != null) {
                    return scenarioVo.getId();
                } else {
                    try {
                        Long scenarioId = Long.valueOf(scenarioName);
                        if (autoexecScenarioMapper.checkScenarioIsExistsById(scenarioId) > 0) {
                            return scenarioId;
                        }
                    } catch (NumberFormatException ignored) {

                    }
                }
            } else if (scenario instanceof Long) {
                Long scenarioId = (Long) scenario;
                if (autoexecScenarioMapper.checkScenarioIsExistsById(scenarioId) > 0) {
                    return scenarioId;
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
    private String getJobNamePrefixValue(String jobNamePrefixKey, AutoexecCombopExecuteConfigVo executeConfig, JSONObject param) {
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

    private JSONArray getTbodyList(ProcessTaskFormAttributeDataVo formAttributeDataVo, JSONArray filterList, Map<String, FormAttributeVo> formAttributeMap) {
        JSONArray tbodyList = new JSONArray();
        if (formAttributeDataVo == null) {
            return tbodyList;
        }
        if (!Objects.equals(formAttributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLEINPUTER.getHandler())
                && !Objects.equals(formAttributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLESELECTOR.getHandler())) {
            return tbodyList;
        }
        if (formAttributeDataVo.getDataObj() == null) {
            return tbodyList;
        }
        JSONArray dataList = new JSONArray();
        JSONArray tempList = (JSONArray) formAttributeDataVo.getDataObj();
        for (int i = 0; i < tempList.size(); i++) {
            JSONObject newRowData = new JSONObject();
            JSONObject rowData = tempList.getJSONObject(i);
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                FormAttributeVo formAttributeVo = formAttributeMap.get(entry.getKey());
                if (formAttributeVo != null) {
                    IFormAttributeDataConversionHandler formAttributeDataConversionHandler = FormAttributeDataConversionHandlerFactory.getHandler(formAttributeVo.getHandler());
                    if (formAttributeDataConversionHandler != null) {
                        newRowData.put(entry.getKey(), formAttributeDataConversionHandler.getSimpleValue(entry.getValue()));
                    } else {
                        newRowData.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    newRowData.put(entry.getKey(), entry.getValue());
                }
            }
            dataList.add(newRowData);
        }
        // 数据过滤
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
                    } else if (Objects.equals(expression, Expression.LIKE.getExpression())) {
                        String columnValue = data.getString(column);
                        if (StringUtils.isBlank(columnValue)) {
                            flag = false;
                            break;
                        }
                        if (!columnValue.contains(value)) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.NOTLIKE.getExpression())) {
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
        return tbodyList;
    }

    private JSONObject getParam(
            ProcessTaskStepVo currentProcessTaskStepVo,
            JSONArray runtimeParamList,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap,
            JSONObject processTaskParam) {
        return getParam(currentProcessTaskStepVo, runtimeParamList, null, formAttributeMap, processTaskFormAttributeDataMap, processTaskParam);
    }

    private JSONObject getParam(
            ProcessTaskStepVo currentProcessTaskStepVo,
            JSONArray runtimeParamList,
            JSONObject tbodyObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap,
            JSONObject processTaskParam) {
        List<String> formSelectAttributeList = new ArrayList<>();
        formSelectAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMSELECT.getHandler());
        formSelectAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMCHECKBOX.getHandler());
        formSelectAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMRADIO.getHandler());
        List<String> formTextAttributeList = new ArrayList<>();
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXT.getHandler());
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXTAREA.getHandler());

        List<String> needFreemarkerReplaceKeyList = new ArrayList<>();
        JSONObject param = new JSONObject();
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
            String type = runtimeParamObj.getString("type");
            String mappingMode = runtimeParamObj.getString("mappingMode");
            if (Objects.equals(mappingMode, "formTableComponent")) {
                String column = runtimeParamObj.getString("column");
                if (tbodyObj != null) {
                    String columnValue = tbodyObj.getString(column);
                    param.put(key, convertDateType(type, columnValue));
                } else {
                    FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    JSONArray filterList = runtimeParamObj.getJSONArray("filterList");
                    JSONArray tbodyList = getTbodyList(attributeDataVo, filterList, formAttributeMap);
                    List<String> list = parseFormTableComponentMappingValue(formAttributeVo, tbodyList, column);
                    param.put(key, convertDateType(type, list));
                }
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                if (attributeDataVo != null) {
                    if (formTextAttributeList.contains(attributeDataVo.getHandler())) {
                        param.put(key, convertDateType(type, (String) attributeDataVo.getDataObj()));
                    } else if (formSelectAttributeList.contains(attributeDataVo.getHandler())) {
                        if (attributeDataVo.getDataObj() instanceof String) {
                            param.put(key, convertDateType(type, (String) attributeDataVo.getDataObj()));
                        } else if (attributeDataVo.getDataObj() instanceof JSONArray) {
                            param.put(key, convertDateType(type, JSONObject.toJSONString(attributeDataVo.getDataObj())));
                        }
                    } else if (Objects.equals(attributeDataVo.getHandler(), neatlogic.framework.form.constvalue.FormHandler.FORMUSERSELECT.getHandler()) && Objects.equals(type, ParamType.USERSELECT.getValue())) {
                        Object dataObj = attributeDataVo.getDataObj();
                        if (dataObj instanceof JSONArray) {
                            param.put(key, dataObj);
                        } else {
                            JSONArray array = new JSONArray();
                            array.add(dataObj);
                            param.put(key, array);
                        }
                    } else if (Objects.equals(type, ParamType.FILE.getValue())) {
                        param.put(key, convertDateTypeForFile(attributeDataVo.getDataObj()));
                    }
                    else {
                        param.put(key, attributeDataVo.getDataObj());
                    }
                }
            } else if (Objects.equals(mappingMode, "constant")) {
                param.put(key, value);
            } else if (Objects.equals(mappingMode, "processTaskParam")) {
                param.put(key, processTaskParam.get(value));
            } else if (Objects.equals(mappingMode, "expression")) {
                if (value instanceof JSONArray) {
                    param.put(key, parseExpression((JSONArray) value, tbodyObj, processTaskFormAttributeDataMap, processTaskParam));
                } else {
                    param.put(key, value);
                }
            }

            if (Objects.equals(type, ParamType.TEXT.getValue()) || Objects.equals(type, ParamType.TEXTAREA.getValue())) {
                Object obj = param.get(key);
                if (obj != null) {
                    String str = obj.toString();
                    if (str.contains("${DATA.stepId}") || str.contains("${DATA.stepWorker}") || str.contains("${DATA.serialNumber}")) {
                        needFreemarkerReplaceKeyList.add(key);
                    }
                }
            }
        }
        // 通过freemarker语法替换参数${DATA.stepId}、${DATA.stepWorker}、${DATA.serialNumber}
        if (CollectionUtils.isNotEmpty(needFreemarkerReplaceKeyList)) {
            for (String key : needFreemarkerReplaceKeyList) {
                JSONObject paramObj = new JSONObject();
                {
                    INotifyParamHandler notifyParamHandler = NotifyParamHandlerFactory.getHandler(ProcessTaskStepNotifyParam.STEPWORKER.getValue());
                    if (notifyParamHandler != null) {
                        Object stepWorker = notifyParamHandler.getText(currentProcessTaskStepVo, ProcessTaskStepNotifyTriggerType.SUCCEED);
                        paramObj.put(ProcessTaskStepNotifyParam.STEPWORKER.getValue(), stepWorker);
                    }
                }
                {
                    INotifyParamHandler notifyParamHandler = NotifyParamHandlerFactory.getHandler(ProcessTaskStepNotifyParam.STEPID.getValue());
                    if (notifyParamHandler != null) {
                        Object stepId = notifyParamHandler.getText(currentProcessTaskStepVo, ProcessTaskStepNotifyTriggerType.SUCCEED);
                        paramObj.put(ProcessTaskStepNotifyParam.STEPID.getValue(), stepId);
                    }
                }
                {
                    INotifyParamHandler notifyParamHandler = NotifyParamHandlerFactory.getHandler(ProcessTaskNotifyParam.SERIALNUMBER.getValue());
                    if (notifyParamHandler != null) {
                        Object serialnumber = notifyParamHandler.getText(currentProcessTaskStepVo, null);
                        paramObj.put(ProcessTaskNotifyParam.SERIALNUMBER.getValue(), serialnumber);
                    }
                }
                Object obj = param.get(key);
                if (obj == null) {
                    continue;
                }
                String str = obj.toString();
                if (str.contains("${DATA.stepId}") || str.contains("${DATA.stepWorker}") || str.contains("${DATA.serialNumber}")) {
                    param.put(key, FreemarkerUtil.transform(paramObj, str));
                }
            }
        }
        return param;
    }

    /**
     * 解析出表达式的值
     * @param valueList
     * @param tbodyObj
     * @param processTaskFormAttributeDataMap
     * @param processTaskParam
     * @return
     */
    private String parseExpression(JSONArray valueList, JSONObject tbodyObj, Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap, JSONObject processTaskParam) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < valueList.size(); i++) {
            JSONObject valueObj = valueList.getJSONObject(i);
            if (MapUtils.isEmpty(valueObj)) {
                continue;
            }
            String value = valueObj.getString("value");
            String mappingMode = valueObj.getString("mappingMode");
            if (Objects.equals(mappingMode, "formTableComponent")) {
                if (tbodyObj != null) {
                    String column = valueObj.getString("column");
                    Object valueObject = tbodyObj.get(column);
                    if (valueObject == null) {
                        continue;
                    }
                    if (valueObject instanceof JSONArray) {
                        List<String> list = new ArrayList<>();
                        JSONArray valueObjectArray = (JSONArray) valueObject;
                        for (int j = 0; j < valueObjectArray.size(); j++) {
                            list.add(valueObjectArray.getString(j));
                        }
                        stringBuilder.append(String.join(",", list));
                    } else {
                        stringBuilder.append(valueObject);
                    }
                }
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                if (attributeDataVo != null && attributeDataVo.getDataObj() != null) {
                    Object dataObject = attributeDataVo.getDataObj();
                    if (dataObject instanceof JSONArray) {
                        List<String> list = new ArrayList<>();
                        JSONArray dataObjectArray = (JSONArray) dataObject;
                        for (int j = 0; j < dataObjectArray.size(); j++) {
                            list.add(dataObjectArray.getString(j));
                        }
                        stringBuilder.append(String.join(",", list));
                    } else {
                        stringBuilder.append(dataObject);
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

    private AutoexecCombopExecuteConfigVo getAutoexecCombopExecuteConfig(
            JSONArray executeParamList,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        return getAutoexecCombopExecuteConfig(executeParamList, null, formAttributeMap, processTaskFormAttributeDataMap);
    }

    private AutoexecCombopExecuteConfigVo getAutoexecCombopExecuteConfig(
            JSONArray executeParamList,
            JSONObject tbodyObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        List<String> formTextAttributeList = new ArrayList<>();
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXT.getHandler());
        formTextAttributeList.add(neatlogic.framework.form.constvalue.FormHandler.FORMTEXTAREA.getHandler());
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
                // 映射模式为表单表格组件
                if (Objects.equals(key, "executeNodeConfig")) {
                    AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
                    String column = executeParamObj.getString("column");
                    if (tbodyObj != null) {
                        String columnValue = tbodyObj.getString(column);
                        if (StringUtils.isNotBlank(columnValue)) {
                            List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
                            inputNodeList.add(new AutoexecNodeVo(columnValue));
                            executeNodeConfigVo.setInputNodeList(inputNodeList);
                        }
                    } else {
                        FormAttributeVo formAttributeVo = formAttributeMap.get(value);
                        ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                        JSONArray filterList = executeParamObj.getJSONArray("filterList");
                        JSONArray tbodyList = getTbodyList(attributeDataVo, filterList, formAttributeMap);
                        List<String> list = parseFormTableComponentMappingValue(formAttributeVo, tbodyList, column);
                        if (CollectionUtils.isNotEmpty(list)) {
                            List<AutoexecNodeVo> inputNodeList = new ArrayList<>();
                            for (String str : list) {
                                inputNodeList.add(new AutoexecNodeVo(str));
                            }
                            executeNodeConfigVo.setInputNodeList(inputNodeList);
                        }
                    }
                    executeConfig.put(key, executeNodeConfigVo);
                }
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                // 映射模式为表单普通组件
                if (Objects.equals(key, "executeUser")) {
                    // 执行用户
                    ParamMappingVo paramMappingVo = new ParamMappingVo();
                    paramMappingVo.setMappingMode("constant");
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    if (attributeDataVo != null) {
                        paramMappingVo.setValue(attributeDataVo.getDataObj());
                    }
                    executeConfig.put(key, paramMappingVo);
                } else if (Objects.equals(key, "executeNodeConfig")) {
                    // 执行目标
                    AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    if (attributeDataVo != null) {
                        Object dataObj = attributeDataVo.getDataObj();
                        if (dataObj != null) {
                            if (Objects.equals(attributeDataVo.getHandler(), FormHandler.FORMRESOURECES.getHandler())) {
                                // 映射的表单组件是执行目标
                                executeNodeConfigVo = ((JSONObject) dataObj).toJavaObject(AutoexecCombopExecuteNodeConfigVo.class);
                            } else if (formTextAttributeList.contains(attributeDataVo.getHandler())) {
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
                        }
                    }
                    executeConfig.put(key, executeNodeConfigVo);
                } else if (Objects.equals(key, "protocolId")) {
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    Object formData = attributeDataVo.getDataObj();
                    try {
                        executeConfig.put(key, Long.valueOf(formData.toString()));
                    } catch (NumberFormatException ex) {
                        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
                        AccountProtocolVo protocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolName(formData.toString());
                        if (protocolVo != null) {
                            executeConfig.put(key, protocolVo.getId());
                        }
                    }
                } else {
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(value);
                    if (attributeDataVo != null) {
                        executeConfig.put(key, attributeDataVo.getDataObj());
                    }
                }
            } else if (Objects.equals(mappingMode, "constant")) {
                // 映射模式为常量
                if (Objects.equals(key, "executeUser")) {
                    ParamMappingVo paramMappingVo = new ParamMappingVo();
                    paramMappingVo.setMappingMode("constant");
                    paramMappingVo.setValue(value);
                    executeConfig.put(key, paramMappingVo);
                } else {
                    executeConfig.put(key, value);
                }
            } else if (Objects.equals(mappingMode, "runtimeparam")) {
                // 映射模式为作业参数，只读
                if (Objects.equals(key, "executeUser")) {
                    ParamMappingVo paramMappingVo = new ParamMappingVo();
                    paramMappingVo.setMappingMode("runtimeparam");
                    paramMappingVo.setValue(value);
                    executeConfig.put(key, paramMappingVo);
                }
            }
        }
        return executeConfig.toJavaObject(AutoexecCombopExecuteConfigVo.class);
    }

    private List<String> parseFormTableComponentMappingValue(FormAttributeVo formAttributeVo, JSONArray tbodyList, String column) {
        List<String> resultList = new ArrayList<>();
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
        jobVo.setRouteId(currentProcessTaskStepVo.getId().toString());
        jobVo.setIsFirstFire(1);
        jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
        return jobVo;
    }

    private void processTaskStepComplete(Long processTaskStepId, Long autoexecJobId) {
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
        List<Long> toProcessTaskStepIdList = processTaskCrossoverMapper.getToProcessTaskStepIdListByFromIdAndType(processTaskStepId, ProcessFlowDirection.FORWARD.getValue());
        if (toProcessTaskStepIdList.size() == 1) {
            Long nextStepId = toProcessTaskStepIdList.get(0);
            IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(AutoexecProcessStepHandlerType.AUTOEXEC.getHandler());
            if (handler != null) {
                try {
                    List<String> hidecomponentList = new ArrayList<>();
                    JSONArray formAttributeDataList = new JSONArray();
                    ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
                    String config = selectContentByHashCrossoverMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
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
                            IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
                            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(processTaskStepVo.getProcessTaskId());
                            for (ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo : processTaskFormAttributeDataList) {
                                JSONObject formAttributeDataObj = new JSONObject();
                                String attributeUuid = processTaskFormAttributeDataVo.getAttributeUuid();
                                formAttributeDataObj.put("attributeUuid", attributeUuid);
                                formAttributeDataObj.put("handler", processTaskFormAttributeDataVo.getHandler());
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
                    paramObj.put("action", ProcessTaskStepOperationType.STEP_COMPLETE.getValue());
                    if (CollectionUtils.isNotEmpty(formAttributeDataList)) {
                        paramObj.put("formAttributeDataList", formAttributeDataList);
                    }
                    if (CollectionUtils.isNotEmpty(hidecomponentList)) {
                        paramObj.put("hidecomponentList", hidecomponentList);
                    }
                    /* 自动处理 **/
                    doNext(ProcessTaskStepOperationType.STEP_COMPLETE, new ProcessStepThread(processTaskStepVo) {
                        @Override
                        public void myExecute() {
                            UserContext.init(SystemUser.SYSTEM);
                            handler.autoComplete(processTaskStepVo);
                        }
                    });
                } catch (ProcessTaskNoPermissionException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } else {
            ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
            IProcessStepHandler processStepHandler = ProcessStepHandlerFactory.getHandler(processTaskStepVo.getHandler());
            if (processStepHandler != null) {
                try {
                    processStepHandler.assign(processTaskStepVo);
                } catch (ProcessTaskException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private Object parseMappingValue(ProcessTaskStepVo currentProcessTaskStepVo, String mappingMode, Object value) {
        if ("form".equals(mappingMode)) {
            IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataVoList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
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
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        String[] split = paramKey.split("&&", 2);
        String processStepUuid = split[0];
        ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoByProcessTaskIdAndProcessStepUuid(processTaskId, processStepUuid);
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

    /**
     * 把表单表格组件中某列数据集合转换成作业参数对应的数据
     *
     * @param paramType  作业参数类型
     * @param sourceList 某列数据集合
     * @return
     */
    private Object convertDateType(String paramType, List<String> sourceList) {
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
    private Object convertDateType(String paramType, String source) {
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

    /**
     * 将数据转换成文件类型参数需要的格式
     * @param dataObj
     * @return
     */
    private JSONObject convertDateTypeForFile(Object dataObj) {
        if (dataObj == null) {
            return null;
        }
        if (dataObj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) dataObj;
            Long fileId = jsonObject.getLong("id");
            return convertDateTypeForFile(fileId);
        } else if (dataObj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) dataObj;
            if (!jsonArray.isEmpty()) {
                JSONObject resultObj = new JSONObject();
                JSONArray fileIdList = new JSONArray();
                JSONArray fileList = new JSONArray();
                for (Object obj : jsonArray) {
                    JSONObject jsonObj = convertDateTypeForFile(obj);
                    if (jsonObj != null) {
                        JSONArray fileIdArray = jsonObj.getJSONArray("fileIdList");
                        fileIdList.addAll(fileIdArray);
                        JSONArray fileArray = jsonObj.getJSONArray("fileList");
                        fileList.addAll(fileArray);
                    }
                }
                resultObj.put("fileIdList", fileIdList);
                resultObj.put("fileList", fileList);
                return resultObj;
            }
        } else if (dataObj instanceof Long) {
            Long fileId = (Long) dataObj;
            FileVo file = fileMapper.getFileById(fileId);
            if (file != null) {
                JSONObject resultObj = new JSONObject();
                JSONArray fileIdList = new JSONArray();
                fileIdList.add(fileId);
                JSONArray fileList = new JSONArray();
                JSONObject fileObj = new JSONObject();
                fileObj.put("id", fileId);
                fileObj.put("name", file.getName());
                fileList.add(fileObj);
                resultObj.put("fileIdList", fileIdList);
                resultObj.put("fileList", fileList);
                return resultObj;
            }
        } else {
            String str = dataObj.toString();
            try {
                Long fileId = Long.valueOf(str);
                return convertDateTypeForFile(fileId);
            } catch (NumberFormatException e) {

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
        return 10;
    }

    @Override
    protected int myBeforeComplete(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
        Long processTaskStepId = currentProcessTaskStepVo.getId();
        ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
        String config = selectContentByHashCrossoverMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
        if (StringUtils.isBlank(config)) {
            return 0;
        }
        JSONArray configList = (JSONArray) JSONPath.read(config, "autoexecConfig.configList");
        if (CollectionUtils.isEmpty(configList)) {
            return 0;
        }
        List<Long> jobIdList = autoexecJobMapper.getJobIdListByProcessTaskStepId(processTaskStepId);
        if (CollectionUtils.isEmpty(jobIdList)) {
            return 0;
        }
        List<AutoexecJobEnvVo> autoexecJobEnvVoList = autoexecJobMapper.getAutoexecJobEnvListByJobIdList(jobIdList);

        Map<String, List<String>> autoexecJobEnvMap = new HashMap<>();
        for (AutoexecJobEnvVo autoexecJobEnvVo : autoexecJobEnvVoList) {
            autoexecJobEnvMap.computeIfAbsent(autoexecJobEnvVo.getName(), k -> new ArrayList<>()).add(autoexecJobEnvVo.getValue());
        }
        Map<String, List<String>> formAttributeNewDataMap = new HashMap<>();
        for (int j = 0; j < configList.size(); j++) {
            JSONObject configObj = configList.getJSONObject(j);
            if (MapUtils.isEmpty(configObj)) {
                continue;
            }
            JSONArray formAttributeList = configObj.getJSONArray("formAttributeList");
            if (CollectionUtils.isEmpty(formAttributeList)) {
                continue;
            }
            for (int i = 0; i < formAttributeList.size(); i++) {
                JSONObject formAttributeObj = formAttributeList.getJSONObject(i);
                String key = formAttributeObj.getString("key");
                if (StringUtils.isBlank(key)) {
                    continue;
                }
                String value = formAttributeObj.getString("value");
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                List<String> newValue = autoexecJobEnvMap.get(value);
                if (newValue != null) {
                    formAttributeNewDataMap.put(key, newValue);
                }
            }
        }
        if (MapUtils.isEmpty(formAttributeNewDataMap)) {
            return 0;
        }
        IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
        List<FormAttributeVo> formAttributeList = processTaskCrossoverService.getFormAttributeListByProcessTaskId(processTaskStepVo.getProcessTaskId());
        if (CollectionUtils.isEmpty(formAttributeList)) {
            return 0;
        }


        Map<String, FormAttributeVo> formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
        JSONObject paramObj = currentProcessTaskStepVo.getParamObj();
        JSONArray formAttributeDataList = paramObj.getJSONArray("formAttributeDataList");
        if (formAttributeDataList == null) {
            formAttributeDataList = new JSONArray();
            List<String> hidecomponentList = formAttributeList.stream().map(FormAttributeVo::getUuid).collect(Collectors.toList());
            paramObj.put("hidecomponentList", hidecomponentList);
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(processTaskStepVo.getProcessTaskId());
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap = processTaskFormAttributeDataList.stream().collect(Collectors.toMap(AttributeDataVo::getAttributeUuid, e -> e));
            for (Map.Entry<String, ProcessTaskFormAttributeDataVo> entry : processTaskFormAttributeDataMap.entrySet()) {
                ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo = entry.getValue();
                JSONObject formAttributeDataObj = new JSONObject();
                formAttributeDataObj.put("attributeUuid", processTaskFormAttributeDataVo.getAttributeUuid());
                formAttributeDataObj.put("handler", processTaskFormAttributeDataVo.getHandler());
                formAttributeDataObj.put("dataList", processTaskFormAttributeDataVo.getDataObj());
                formAttributeDataList.add(formAttributeDataObj);
            }
        }

        for (Map.Entry<String, List<String>> entry : formAttributeNewDataMap.entrySet()) {
            String attributeUuid = entry.getKey();
            FormAttributeVo formAttributeVo = formAttributeMap.get(attributeUuid);
            if (formAttributeVo == null) {
                continue;
            }
            List<String> newDataList = entry.getValue();
            if (CollectionUtils.isEmpty(newDataList)) {
                continue;
            }

            JSONObject formAttributeDataObj = null;
            for (int i = 0; i < formAttributeDataList.size(); i++) {
                JSONObject tempObj = formAttributeDataList.getJSONObject(i);
                if (Objects.equals(tempObj.getString("attributeUuid"), attributeUuid)) {
                    formAttributeDataObj = tempObj;
                }
            }
            if (formAttributeDataObj == null) {
                formAttributeDataObj = new JSONObject();
                formAttributeDataList.add(formAttributeDataObj);
            }
            formAttributeDataObj.put("attributeUuid", attributeUuid);
            formAttributeDataObj.put("handler", formAttributeVo.getHandler());
            if (newDataList.size() == 1) {
                // 如果只有一个元素，把唯一的元素取出来赋值给表单组件
                formAttributeDataObj.put("dataList", newDataList.get(0));
            } else {
                // 如果有多个元素，分为两种情况
                // 1.元素也是一个数组，需要把所有元素（数组）平摊成一个大一维数组
                // 2.元素不是一个数组，不需要特殊处理
                JSONArray newDataArray = new JSONArray();
                for (String newData : newDataList) {
                    try {
                        JSONArray array = JSON.parseArray(newData);
                        newDataArray.addAll(array);
                    } catch (JSONException e) {
                        newDataArray.add(newData);
                    }
                }
                formAttributeDataObj.put("dataList", JSON.toJSONString(newDataArray));
            }
        }
        paramObj.put("formAttributeDataList", formAttributeDataList);
        return 0;
    }

    @Override
    protected int myCompleteAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        if (StringUtils.isNotBlank(currentProcessTaskStepVo.getError())) {
            currentProcessTaskStepVo.getParamObj().put(ProcessTaskAuditDetailType.CAUSE.getParamName(), currentProcessTaskStepVo.getError());
        }
        /** 处理历史记录 **/
        String action = currentProcessTaskStepVo.getParamObj().getString("action");
        IProcessStepHandlerCrossoverUtil processStepHandlerCrossoverUtil = CrossoverServiceFactory.getApi(IProcessStepHandlerCrossoverUtil.class);
        processStepHandlerCrossoverUtil.audit(currentProcessTaskStepVo, ProcessTaskAuditType.getProcessTaskAuditType(action));
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

    @Override
    public boolean disableAssign() {
        return true;
    }

    @Override
    public boolean allowDispatchStepWorker() {
        return false;
    }
}
