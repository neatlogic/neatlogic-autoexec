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

package neatlogic.module.autoexec.stephandler.utilhandler;

import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.dto.ProcessStepVo;
import neatlogic.framework.process.dto.ProcessStepWorkerPolicyVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.processconfig.ActionConfigActionVo;
import neatlogic.framework.process.dto.processconfig.ActionConfigVo;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import neatlogic.framework.process.util.ProcessConfigUtil;
import neatlogic.framework.process.constvalue.AutoexecProcessStepHandlerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.module.autoexec.notify.handler.AutoexecCombopNotifyPolicyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/9/2 14:30
 **/
@Service
public class AutoexecProcessUtilHandler extends ProcessStepInternalHandlerBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getHandler() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getHandler();
    }

    @Override
    public Object getHandlerStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return getHandlerStepInitInfo(currentProcessTaskStepVo);
    }

    @Override
    public Object getHandlerStepInitInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
        if (autoexecJobId != null) {
            AutoexecJobVo autoexecJobVo = autoexecJobMapper.getJobInfo(autoexecJobId);
            if (autoexecJobVo != null) {
                List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(autoexecJobId);
                autoexecJobVo.setPhaseList(jobPhaseVoList);
                return autoexecJobVo;
            }
        }
        return null;
    }

    @Override
    public void makeupProcessStep(ProcessStepVo processStepVo, JSONObject stepConfigObj) {
        /* 组装通知策略id **/
        JSONObject notifyPolicyConfig = stepConfigObj.getJSONObject("notifyPolicyConfig");
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, InvokeNotifyPolicyConfigVo.class);
        if (invokeNotifyPolicyConfigVo != null) {
            processStepVo.setNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
        }

        JSONObject actionConfig = stepConfigObj.getJSONObject("actionConfig");
        ActionConfigVo actionConfigVo = JSONObject.toJavaObject(actionConfig, ActionConfigVo.class);
        if (actionConfigVo != null) {
            List<ActionConfigActionVo> actionList = actionConfigVo.getActionList();
            if (CollectionUtils.isNotEmpty(actionList)) {
                List<String> integrationUuidList = new ArrayList<>();
                for (ActionConfigActionVo actionVo : actionList) {
                    String integrationUuid = actionVo.getIntegrationUuid();
                    if (StringUtils.isNotBlank(integrationUuid)) {
                        integrationUuidList.add(integrationUuid);
                    }
                }
                processStepVo.setIntegrationUuidList(integrationUuidList);
            }
        }

        /** 组装分配策略 **/
        JSONObject workerPolicyConfig = stepConfigObj.getJSONObject("workerPolicyConfig");
        if (MapUtils.isNotEmpty(workerPolicyConfig)) {
            JSONArray policyList = workerPolicyConfig.getJSONArray("policyList");
            if (CollectionUtils.isNotEmpty(policyList)) {
                List<ProcessStepWorkerPolicyVo> workerPolicyList = new ArrayList<>();
                for (int k = 0; k < policyList.size(); k++) {
                    JSONObject policyObj = policyList.getJSONObject(k);
                    if (!"1".equals(policyObj.getString("isChecked"))) {
                        continue;
                    }
                    ProcessStepWorkerPolicyVo processStepWorkerPolicyVo = new ProcessStepWorkerPolicyVo();
                    processStepWorkerPolicyVo.setProcessUuid(processStepVo.getProcessUuid());
                    processStepWorkerPolicyVo.setProcessStepUuid(processStepVo.getUuid());
                    processStepWorkerPolicyVo.setPolicy(policyObj.getString("type"));
                    processStepWorkerPolicyVo.setSort(k + 1);
                    processStepWorkerPolicyVo.setConfig(policyObj.getString("config"));
                    workerPolicyList.add(processStepWorkerPolicyVo);
                }
                processStepVo.setWorkerPolicyList(workerPolicyList);
            }
        }

        JSONArray tagList = stepConfigObj.getJSONArray("tagList");
        if (CollectionUtils.isNotEmpty(tagList)) {
            processStepVo.setTagList(tagList.toJavaList(String.class));
        }
        // 保存表单场景
        String formSceneUuid = stepConfigObj.getString("formSceneUuid");
        if (StringUtils.isNotBlank(formSceneUuid)) {
            processStepVo.setFormSceneUuid(formSceneUuid);
        }
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
        ProcessTaskOperationType[] stepActions = {
                ProcessTaskOperationType.STEP_VIEW,
                ProcessTaskOperationType.STEP_TRANSFER
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

        /* 按钮映射 **/
        ProcessTaskOperationType[] stepButtons = {
                ProcessTaskOperationType.STEP_COMPLETE,
                ProcessTaskOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskOperationType.STEP_ACCEPT
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

        /* 通知 **/
        JSONObject notifyPolicyConfig = configObj.getJSONObject("notifyPolicyConfig");
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, InvokeNotifyPolicyConfigVo.class);
        if (invokeNotifyPolicyConfigVo == null) {
            invokeNotifyPolicyConfigVo = new InvokeNotifyPolicyConfigVo();
        }
        invokeNotifyPolicyConfigVo.setHandler(AutoexecCombopNotifyPolicyHandler.class.getName());
        resultObj.put("notifyPolicyConfig", invokeNotifyPolicyConfigVo);

        return resultObj;
    }

    @Override
    public JSONObject regulateProcessStepConfig(JSONObject configObj) {
        if (configObj == null) {
            configObj = new JSONObject();
        }
        JSONObject resultObj = new JSONObject();

        /* 授权 **/
        ProcessTaskOperationType[] stepActions = {
                ProcessTaskOperationType.STEP_VIEW,
                ProcessTaskOperationType.STEP_TRANSFER
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
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, InvokeNotifyPolicyConfigVo.class);
        if (invokeNotifyPolicyConfigVo == null) {
            invokeNotifyPolicyConfigVo = new InvokeNotifyPolicyConfigVo();
        }
        invokeNotifyPolicyConfigVo.setHandler(AutoexecCombopNotifyPolicyHandler.class.getName());
        resultObj.put("notifyPolicyConfig", invokeNotifyPolicyConfigVo);

        /** 动作 **/
        JSONObject actionConfig = configObj.getJSONObject("actionConfig");
        ActionConfigVo actionConfigVo = JSONObject.toJavaObject(actionConfig, ActionConfigVo.class);
        if (actionConfigVo == null) {
            actionConfigVo = new ActionConfigVo();
        }
        actionConfigVo.setHandler(AutoexecCombopNotifyPolicyHandler.class.getName());
        resultObj.put("actionConfig", actionConfigVo);

        /* 按钮映射列表 **/
        ProcessTaskOperationType[] stepButtons = {
                ProcessTaskOperationType.STEP_COMPLETE,
                ProcessTaskOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskOperationType.STEP_ACCEPT
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
        JSONObject autoexecObj = new JSONObject();
        JSONObject autoexecConfig = configObj.getJSONObject("autoexecConfig");
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
}