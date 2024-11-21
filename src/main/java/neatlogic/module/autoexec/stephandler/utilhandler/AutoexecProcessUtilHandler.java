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

package neatlogic.module.autoexec.stephandler.utilhandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.process.constvalue.AutoexecProcessStepHandlerType;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.crossover.IProcessTaskStepDataCrossoverMapper;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.processconfig.ActionConfigVo;
import neatlogic.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import neatlogic.framework.process.util.ProcessConfigUtil;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.autoexec.notify.handler.AutoexecNotifyPolicyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author linbq
 * @since 2021/9/2 14:30
 **/
//@Service
@Deprecated
public class AutoexecProcessUtilHandler extends ProcessStepInternalHandlerBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getHandler() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getHandler();
    }

    @Override
    public Object getStartStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return getNonStartStepInfo(currentProcessTaskStepVo);
    }

    @Override
    public Object getNonStartStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        JSONObject resultObj = new JSONObject();
//        List<Long> jobIdList = autoexecJobMapper.getJobIdListByInvokeId(currentProcessTaskStepVo.getId());
        List<Long> jobIdList = autoexecJobMapper.getJobIdListByProcessTaskStepId(currentProcessTaskStepVo.getId());
        if (CollectionUtils.isNotEmpty(jobIdList)) {
            int completed = 0, failed = 0, running = 0;
            Map<Long, List<AutoexecJobPhaseVo>> jobIdToAutoexecJobPhaseListMap = new HashMap<>();
            List<AutoexecJobPhaseVo> jobPhaseList = autoexecJobMapper.getJobPhaseListWithGroupByJobIdList(jobIdList);
            for (AutoexecJobPhaseVo autoexecJobPhaseVo : jobPhaseList) {
                jobIdToAutoexecJobPhaseListMap.computeIfAbsent(autoexecJobPhaseVo.getJobId(), key -> new ArrayList<>()).add(autoexecJobPhaseVo);
            }
            List<AutoexecJobVo> autoexecJobList = autoexecJobMapper.getJobListByIdList(jobIdList);
            for (AutoexecJobVo autoexecJobVo : autoexecJobList) {
                List<AutoexecJobPhaseVo> jobPhaseVoList = jobIdToAutoexecJobPhaseListMap.get(autoexecJobVo.getId());
                autoexecJobVo.setPhaseList(jobPhaseVoList);
                if (JobStatus.isRunningStatus(autoexecJobVo.getStatus())) {
                    running++;
                } else if (JobStatus.isCompletedStatus(autoexecJobVo.getStatus())) {
                    completed++;
                } else if (JobStatus.isFailedStatus(autoexecJobVo.getStatus())) {
                    failed++;
                }
            }

            if (running > 0) {
                resultObj.put("status", JobStatus.RUNNING.getValue());
            } else if (failed > 0) {
                resultObj.put("status", JobStatus.FAILED.getValue());
            } else if (completed > 0) {
                resultObj.put("status", JobStatus.COMPLETED.getValue());
            }
            resultObj.put("jobList", autoexecJobList);
        }
        IProcessTaskStepDataCrossoverMapper processTaskStepDataCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskStepDataCrossoverMapper.class);
        ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
        searchVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
        searchVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
        searchVo.setType("autoexecCreateJobError");
        ProcessTaskStepDataVo processTaskStepDataVo = processTaskStepDataCrossoverMapper.getProcessTaskStepData(searchVo);
        if (processTaskStepDataVo != null) {
            JSONObject dataObj = processTaskStepDataVo.getData();
            if (MapUtils.isNotEmpty(dataObj)) {
                JSONArray errorList = dataObj.getJSONArray("errorList");
                if (CollectionUtils.isNotEmpty(errorList)) {
                    resultObj.put("errorList", errorList);
                }
            }
        }
        return resultObj;
    }


    @Override
    public void updateProcessTaskStepUserAndWorker(Long processTaskId, Long processTaskStepId) {

    }

    @Override
    public JSONObject makeupConfig(JSONObject configObj) {
        if (configObj == null) {
            configObj = new JSONObject();
        }
        JSONObject resultObj = new JSONObject();

        /* 授权 **/
        IOperationType[] stepActions = {
                ProcessTaskStepOperationType.STEP_VIEW,
                ProcessTaskStepOperationType.STEP_TRANSFER
        };
        JSONArray authorityList = configObj.getJSONArray("authorityList");
        JSONArray authorityArray = ProcessConfigUtil.regulateAuthorityList(authorityList, stepActions);
        resultObj.put("authorityList", authorityArray);

        /* 按钮映射 **/
        IOperationType[] stepButtons = {
                ProcessTaskStepOperationType.STEP_COMPLETE,
                ProcessTaskStepOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskStepOperationType.STEP_ACCEPT
        };
        JSONArray customButtonList = configObj.getJSONArray("customButtonList");
        JSONArray customButtonArray = ProcessConfigUtil.regulateCustomButtonList(customButtonList, stepButtons);
        resultObj.put("customButtonList", customButtonArray);
        /* 状态映射列表 **/
        JSONArray customStatusList = configObj.getJSONArray("customStatusList");
        JSONArray customStatusArray = ProcessConfigUtil.regulateCustomStatusList(customStatusList);
        resultObj.put("customStatusList", customStatusArray);

        /* 可替换文本列表 **/
        resultObj.put("replaceableTextList", ProcessConfigUtil.regulateReplaceableTextList(configObj.getJSONArray("replaceableTextList")));
        return resultObj;
    }

    @Override
    public JSONObject regulateProcessStepConfig(JSONObject configObj) {
        if (configObj == null) {
            configObj = new JSONObject();
        }
        JSONObject resultObj = new JSONObject();

        /* 授权 **/
        IOperationType[] stepActions = {
                ProcessTaskStepOperationType.STEP_VIEW,
                ProcessTaskStepOperationType.STEP_TRANSFER
        };
        JSONArray authorityList = null;
        Integer enableAuthority = configObj.getInteger("enableAuthority");
        if (Objects.equals(enableAuthority, 1)) {
            authorityList = configObj.getJSONArray("authorityList");
        } else {
            enableAuthority = 0;
        }
        resultObj.put("enableAuthority", enableAuthority);
        JSONArray authorityArray = ProcessConfigUtil.regulateAuthorityList(authorityList, stepActions);
        resultObj.put("authorityList", authorityArray);

        /* 通知 **/
        JSONObject notifyPolicyConfig = configObj.getJSONObject("notifyPolicyConfig");
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(notifyPolicyConfig, AutoexecNotifyPolicyHandler.class);
        resultObj.put("notifyPolicyConfig", invokeNotifyPolicyConfigVo);

        /** 动作 **/
        JSONObject actionConfig = configObj.getJSONObject("actionConfig");
        ActionConfigVo actionConfigVo = JSONObject.toJavaObject(actionConfig, ActionConfigVo.class);
        if (actionConfigVo == null) {
            actionConfigVo = new ActionConfigVo();
        }
        actionConfigVo.setHandler(AutoexecNotifyPolicyHandler.class.getName());
        resultObj.put("actionConfig", actionConfigVo);

        /* 按钮映射列表 **/
        IOperationType[] stepButtons = {
                ProcessTaskStepOperationType.STEP_COMPLETE,
                ProcessTaskStepOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskStepOperationType.STEP_ACCEPT
        };
        JSONArray customButtonList = configObj.getJSONArray("customButtonList");
        JSONArray customButtonArray = ProcessConfigUtil.regulateCustomButtonList(customButtonList, stepButtons);
        resultObj.put("customButtonList", customButtonArray);
        /* 状态映射列表 **/
        JSONArray customStatusList = configObj.getJSONArray("customStatusList");
        JSONArray customStatusArray = ProcessConfigUtil.regulateCustomStatusList(customStatusList);
        resultObj.put("customStatusList", customStatusArray);

        /* 可替换文本列表 **/
        resultObj.put("replaceableTextList", ProcessConfigUtil.regulateReplaceableTextList(configObj.getJSONArray("replaceableTextList")));

        /* 自动化配置 **/
        JSONObject autoexecConfig = configObj.getJSONObject("autoexecConfig");
        JSONObject autoexecObj = regulateAutoexecConfig(autoexecConfig);
        resultObj.put("autoexecConfig", autoexecObj);

        /** 分配处理人 **/
        JSONObject workerPolicyConfig = configObj.getJSONObject("workerPolicyConfig");
        JSONObject workerPolicyObj = ProcessConfigUtil.regulateWorkerPolicyConfig(workerPolicyConfig);
        resultObj.put("workerPolicyConfig", workerPolicyObj);

        JSONArray tagList = configObj.getJSONArray("tagList");
        if (tagList == null) {
            tagList = new JSONArray();
        }
        resultObj.put("tagList", tagList);
        /** 表单场景 **/
        String formSceneUuid = configObj.getString("formSceneUuid");
        String formSceneName = configObj.getString("formSceneName");
        resultObj.put("formSceneUuid", formSceneUuid == null ? "" : formSceneUuid);
        resultObj.put("formSceneName", formSceneName == null ? "" : formSceneName);
        return resultObj;
    }

    private JSONObject regulateAutoexecConfig(JSONObject autoexecConfig) {
        JSONObject autoexecObj = new JSONObject();
        if (autoexecConfig == null) {
            autoexecConfig = new JSONObject();
        }
        // 失败策略
        String failPolicy = autoexecConfig.getString("failPolicy");
        if (failPolicy == null) {
            failPolicy = StringUtils.EMPTY;
        }
        autoexecObj.put("failPolicy", failPolicy);
        // 回退步骤新建作业
        Integer rerunStepToCreateNewJob = autoexecConfig.getInteger("rerunStepToCreateNewJob");
        if (rerunStepToCreateNewJob == null) {
            rerunStepToCreateNewJob = 0;
        }
        autoexecObj.put("rerunStepToCreateNewJob", rerunStepToCreateNewJob);
        JSONArray configArray = new JSONArray();
        JSONArray configList = autoexecConfig.getJSONArray("configList");
        if (CollectionUtils.isNotEmpty(configList)) {
            for (int i = 0; i < configList.size(); i++) {
                JSONObject config = configList.getJSONObject(i);
                if (MapUtils.isEmpty(config)) {
                    continue;
                }
                String createJobPolicy = config.getString("createJobPolicy");
                if (createJobPolicy == null) {
                    continue;
                }
                JSONObject configObj = new JSONObject();
                configObj.put("createJobPolicy", createJobPolicy);
                Long id = config.getLong("id");
                if (id == null) {
                    id = SnowflakeUtil.uniqueLong();
                }
                configObj.put("id", id);
                String formTag = config.getString("formTag");
                if (StringUtils.isBlank(formTag)) {
                    formTag = StringUtils.EMPTY;
                }
                configObj.put("formTag", formTag);
                Long autoexecCombopId = config.getLong("autoexecCombopId");
                if (autoexecCombopId != null) {
                    configObj.put("autoexecCombopId", autoexecCombopId);
                }
                String jobName = config.getString("jobName");
                if (jobName == null) {
                    jobName = StringUtils.EMPTY;
                }
                configObj.put("jobName", jobName);
                String jobNamePrefix = config.getString("jobNamePrefix");
                if (jobNamePrefix == null) {
                    jobNamePrefix = StringUtils.EMPTY;
                }
                configObj.put("jobNamePrefix", jobNamePrefix);
                Boolean isShow = config.getBoolean("isShow");
                if (isShow == null) {
                    isShow = false;
                }
                configObj.put("isShow", isShow);
                // 批量创建作业
                if (Objects.equals(createJobPolicy, "batch")) {
                    JSONObject batchJobDataSourceObj = new JSONObject();
                    JSONObject batchJobDataSource = config.getJSONObject("batchJobDataSource");
                    if (MapUtils.isNotEmpty(batchJobDataSource)) {
                        String attributeUuid = batchJobDataSource.getString("attributeUuid");
                        if (attributeUuid == null) {
                            attributeUuid = StringUtils.EMPTY;
                        }
                        batchJobDataSourceObj.put("attributeUuid", attributeUuid);
                        JSONArray filterArray = new JSONArray();
                        JSONArray filterList = batchJobDataSource.getJSONArray("filterList");
                        if (CollectionUtils.isNotEmpty(filterList)) {
                            for (int j = 0; j < filterList.size(); j++) {
                                JSONObject filter = filterList.getJSONObject(j);
                                if (MapUtils.isEmpty(filter)) {
                                    continue;
                                }
                                JSONObject filterObj = new JSONObject();
                                filterObj.put("column", filter.getString("column"));
                                filterObj.put("expression", filter.getString("expression"));
                                filterObj.put("value", filter.getString("value"));
                                filterArray.add(filterObj);
                            }
                        }
                        batchJobDataSourceObj.put("filterList", filterArray);
                    }
                    configObj.put("batchJobDataSource", batchJobDataSourceObj);
                }
                JSONArray scenarioParamList = config.getJSONArray("scenarioParamList");
                if (scenarioParamList != null) {
                    JSONArray scenarioParamArray = new JSONArray();
                    for (int j = 0; j < scenarioParamList.size(); j++) {
                        JSONObject scenarioParamObj = scenarioParamList.getJSONObject(j);
                        if (MapUtils.isNotEmpty(scenarioParamObj)) {
                            JSONObject scenarioParam = new JSONObject();
                            scenarioParam.put("key", scenarioParamObj.getString("key"));
                            scenarioParam.put("name", scenarioParamObj.getString("name"));
                            scenarioParam.put("mappingMode", scenarioParamObj.getString("mappingMode"));
                            scenarioParam.put("value", scenarioParamObj.get("value"));
                            scenarioParam.put("column", scenarioParamObj.getString("column"));
                            scenarioParam.put("filterList", scenarioParamObj.getJSONArray("filterList"));
                            scenarioParam.put("isRequired", scenarioParamObj.getInteger("isRequired"));
                            scenarioParamArray.add(scenarioParam);
                        }
                    }
                    configObj.put("scenarioParamList", scenarioParamArray);
                }
                // 作业参数赋值列表
                JSONArray runtimeParamList = config.getJSONArray("runtimeParamList");
                if (runtimeParamList != null) {
                    JSONArray runtimeParamArray = new JSONArray();
                    for (int j = 0; j < runtimeParamList.size(); j++) {
                        JSONObject runtimeParamObj = runtimeParamList.getJSONObject(j);
                        if (MapUtils.isNotEmpty(runtimeParamObj)) {
                            JSONObject runtimeParam = new JSONObject();
                            runtimeParam.put("key", runtimeParamObj.getString("key"));
                            runtimeParam.put("name", runtimeParamObj.getString("name"));
                            runtimeParam.put("mappingMode", runtimeParamObj.getString("mappingMode"));
                            runtimeParam.put("value", runtimeParamObj.get("value"));
                            runtimeParam.put("column", runtimeParamObj.getString("column"));
                            runtimeParam.put("filterList", runtimeParamObj.getJSONArray("filterList"));
                            runtimeParam.put("isRequired", runtimeParamObj.getInteger("isRequired"));
                            runtimeParam.put("type", runtimeParamObj.getString("type"));
                            runtimeParam.put("config", runtimeParamObj.getJSONObject("config"));
                            runtimeParamArray.add(runtimeParam);
                        }
                    }
                    configObj.put("runtimeParamList", runtimeParamArray);
                }
                // 目标参数赋值列表
                JSONArray executeParamList = config.getJSONArray("executeParamList");
                if (executeParamList != null) {
                    JSONArray executeParamArray = new JSONArray();
                    for (int j = 0; j < executeParamList.size(); j++) {
                        JSONObject executeParamObj = executeParamList.getJSONObject(j);
                        if (MapUtils.isNotEmpty(executeParamObj)) {
                            JSONObject executeParam = new JSONObject();
                            executeParam.put("key", executeParamObj.getString("key"));
                            executeParam.put("name", executeParamObj.getString("name"));
                            executeParam.put("mappingMode", executeParamObj.getString("mappingMode"));
                            executeParam.put("value", executeParamObj.get("value"));
                            executeParam.put("column", executeParamObj.getString("column"));
                            executeParam.put("filterList", executeParamObj.getJSONArray("filterList"));
                            executeParam.put("isRequired", executeParamObj.getInteger("isRequired"));
                            executeParamArray.add(executeParam);
                        }
                    }
                    configObj.put("executeParamList", executeParamArray);
                }
                // 导出参数列表
                JSONArray exportParamList = config.getJSONArray("exportParamList");
                if (exportParamList != null) {
                    JSONArray exportParamArray = new JSONArray();
                    for (int j = 0; j < exportParamList.size(); j++) {
                        JSONObject exportParamObj = exportParamList.getJSONObject(j);
                        if (MapUtils.isNotEmpty(exportParamObj)) {
                            JSONObject exportParam = new JSONObject();
                            exportParam.put("value", exportParamObj.getString("value"));
                            exportParam.put("text", exportParamObj.getString("text"));
                            exportParamArray.add(exportParam);
                        }
                    }
                    configObj.put("exportParamList", exportParamArray);
                }
                // 表单赋值列表
                JSONArray formAttributeList = config.getJSONArray("formAttributeList");
                if (formAttributeList != null) {
                    JSONArray formAttributeArray = new JSONArray();
                    for (int j = 0; j < formAttributeList.size(); j++) {
                        JSONObject formAttributeObj = formAttributeList.getJSONObject(j);
                        if (MapUtils.isNotEmpty(formAttributeObj)) {
                            JSONObject formAttribute = new JSONObject();
                            formAttribute.put("key", formAttributeObj.getString("key"));
                            formAttribute.put("name", formAttributeObj.getString("name"));
                            formAttribute.put("value", formAttributeObj.get("value"));
                            formAttributeArray.add(formAttribute);
                        }
                    }
                    configObj.put("formAttributeList", formAttributeArray);
                }

                //执行器组
                configObj.put("runnerGroup", config.getJSONObject("runnerGroup"));
                configArray.add(configObj);
            }
        }
        autoexecObj.put("configList", configArray);
        return autoexecObj;
    }

    private JSONObject getAutoexecConfig(JSONObject autoexecConfig) {
        JSONObject autoexecObj = new JSONObject();
        if (autoexecConfig == null) {
            autoexecConfig = new JSONObject();
        }
        String failPolicy = autoexecConfig.getString("failPolicy");
        if (failPolicy == null) {
            failPolicy = "";
        }
        autoexecObj.put("failPolicy", failPolicy);
        Long autoexecTypeId = autoexecConfig.getLong("autoexecTypeId");
        if (autoexecTypeId != null) {
            autoexecObj.put("autoexecTypeId", autoexecTypeId);
        }
        Long autoexecCombopId = autoexecConfig.getLong("autoexecCombopId");
        if (autoexecCombopId != null) {
            autoexecObj.put("autoexecCombopId", autoexecCombopId);
        }
        JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
        if (runtimeParamList != null) {
            JSONArray runtimeParamArray = new JSONArray();
            for (int i = 0; i < runtimeParamList.size(); i++) {
                JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(runtimeParamObj)) {
                    JSONObject runtimeParam = new JSONObject();
                    runtimeParam.put("key", runtimeParamObj.getString("key"));
                    runtimeParam.put("name", runtimeParamObj.getString("name"));
                    runtimeParam.put("mappingMode", runtimeParamObj.getString("mappingMode"));
                    runtimeParam.put("value", runtimeParamObj.get("value"));
                    runtimeParam.put("isRequired", runtimeParamObj.getInteger("isRequired"));
                    runtimeParam.put("type", runtimeParamObj.getString("type"));
                    runtimeParam.put("config", runtimeParamObj.get("config"));
                    runtimeParamArray.add(runtimeParam);
                }
            }
            autoexecObj.put("runtimeParamList", runtimeParamArray);
        }
        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
        if (executeParamList != null) {
            JSONArray executeParamArray = new JSONArray();
            for (int i = 0; i < executeParamList.size(); i++) {
                JSONObject executeParamObj = executeParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(executeParamObj)) {
                    JSONObject executeParam = new JSONObject();
                    executeParam.put("key", executeParamObj.getString("key"));
                    executeParam.put("name", executeParamObj.getString("name"));
                    executeParam.put("mappingMode", executeParamObj.getString("mappingMode"));
                    executeParam.put("value", executeParamObj.get("value"));
                    executeParam.put("isRequired", executeParamObj.getInteger("isRequired"));
                    executeParamArray.add(executeParam);
                }
            }
            autoexecObj.put("executeParamList", executeParamArray);
        }
        JSONArray exportParamList = autoexecConfig.getJSONArray("exportParamList");
        if (exportParamList != null) {
            JSONArray exportParamArray = new JSONArray();
            for (int i = 0; i < exportParamList.size(); i++) {
                JSONObject exportParamObj = exportParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(exportParamObj)) {
                    JSONObject exportParam = new JSONObject();
                    exportParam.put("value", exportParamObj.getString("value"));
                    exportParam.put("text", exportParamObj.getString("text"));
                    exportParamArray.add(exportParam);
                }
            }
            autoexecObj.put("exportParamList", exportParamArray);
        }
        JSONArray formAttributeList = autoexecConfig.getJSONArray("formAttributeList");
        if (formAttributeList != null) {
            JSONArray formAttributeArray = new JSONArray();
            for (int i = 0; i < formAttributeList.size(); i++) {
                JSONObject formAttributeObj = formAttributeList.getJSONObject(i);
                if (MapUtils.isNotEmpty(formAttributeObj)) {
                    JSONObject formAttribute = new JSONObject();
                    formAttribute.put("key", formAttributeObj.getString("key"));
                    formAttribute.put("name", formAttributeObj.getString("name"));
                    formAttribute.put("value", formAttributeObj.get("value"));
                    formAttributeArray.add(formAttribute);
                }
            }
            autoexecObj.put("formAttributeList", formAttributeArray);
        }
        return autoexecObj;
    }
}
