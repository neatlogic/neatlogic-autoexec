/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.stephandler.utilhandler;

import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.dto.ProcessStepVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.processconfig.ActionConfigVo;
import codedriver.framework.process.dto.processconfig.NotifyPolicyConfigVo;
import codedriver.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import codedriver.framework.process.util.ProcessConfigUtil;
import codedriver.module.autoexec.constvalue.AutoexecProcessStepHandlerType;
import codedriver.module.autoexec.notify.handler.AutoexecCombopNotifyPolicyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author linbq
 * @since 2021/9/2 14:30
 **/
@Service
public class AutoexecProcessUtilHandler extends ProcessStepInternalHandlerBase {
    @Override
    public String getHandler() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getHandler();
    }

    @Override
    public Object getHandlerStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return null;
    }

    @Override
    public Object getHandlerStepInitInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return null;
    }

    @Override
    public void makeupProcessStep(ProcessStepVo processStepVo, JSONObject stepConfigObj) {
        /* 组装通知策略id **/
        JSONObject notifyPolicyConfig = stepConfigObj.getJSONObject("notifyPolicyConfig");
        NotifyPolicyConfigVo notifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, NotifyPolicyConfigVo.class);
        if (notifyPolicyConfigVo != null) {
            Long policyId = notifyPolicyConfigVo.getPolicyId();
            if (policyId != null) {
                processStepVo.setNotifyPolicyId(policyId);
            }
        }

        JSONArray tagList = stepConfigObj.getJSONArray("tagList");
        if (CollectionUtils.isNotEmpty(tagList)) {
            processStepVo.setTagList(tagList.toJavaList(String.class));
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
                ProcessTaskOperationType.STEP_START
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
        NotifyPolicyConfigVo notifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, NotifyPolicyConfigVo.class);
        if (notifyPolicyConfigVo == null) {
            notifyPolicyConfigVo = new NotifyPolicyConfigVo();
        }
        notifyPolicyConfigVo.setHandler(AutoexecCombopNotifyPolicyHandler.class.getName());
        resultObj.put("notifyPolicyConfig", notifyPolicyConfigVo);

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
        NotifyPolicyConfigVo notifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, NotifyPolicyConfigVo.class);
        if (notifyPolicyConfigVo == null) {
            notifyPolicyConfigVo = new NotifyPolicyConfigVo();
        }
        notifyPolicyConfigVo.setHandler(AutoexecCombopNotifyPolicyHandler.class.getName());
        resultObj.put("notifyPolicyConfig", notifyPolicyConfigVo);

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
                ProcessTaskOperationType.STEP_START
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
        if (autoexecTypeId == null) {
            autoexecObj.put("autoexecTypeId", autoexecTypeId);
        }
        Long autoexecCombopId = autoexecConfig.getLong("autoexecCombopId");
        if (autoexecCombopId == null) {
            autoexecObj.put("autoexecCombopId", autoexecCombopId);
        }
        JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            JSONArray runtimeParamArray = new JSONArray();
            for (int i = 0; i < runtimeParamList.size(); i++) {
                JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(runtimeParamObj)) {
                    JSONObject runtimeParam = new JSONObject();
                    runtimeParam.put("key", runtimeParamObj.getString("key"));
                    runtimeParam.put("mappingMode", runtimeParamObj.getString("mappingMode"));
                    runtimeParam.put("value", runtimeParamObj.getString("value"));
                    runtimeParam.put("isRequired", runtimeParamObj.getInteger("isRequired"));
                    runtimeParamArray.add(runtimeParam);
                }
            }
            resultObj.put("runtimeParamList", runtimeParamArray);
        }
        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
        if (CollectionUtils.isNotEmpty(executeParamList)) {
            JSONArray executeParamArray = new JSONArray();
            for (int i = 0; i < executeParamList.size(); i++) {
                JSONObject executeParamObj = executeParamList.getJSONObject(i);
                if (MapUtils.isNotEmpty(executeParamObj)) {
                    JSONObject executeParam = new JSONObject();
                    executeParam.put("key", executeParamObj.getString("key"));
                    executeParam.put("mappingMode", executeParamObj.getString("mappingMode"));
                    executeParam.put("value", executeParamObj.getString("value"));
                    executeParam.put("isRequired", executeParamObj.getInteger("isRequired"));
                    executeParamArray.add(executeParam);
                }
            }
            resultObj.put("executeParamList", executeParamArray);
        }
        JSONArray formAttributeList = autoexecConfig.getJSONArray("formAttributeList");
        if (CollectionUtils.isNotEmpty(formAttributeList)) {
            JSONArray formAttributeArray = new JSONArray();
            for (int i = 0; i < formAttributeList.size(); i++) {
                JSONObject formAttributeObj = formAttributeList.getJSONObject(i);
                if (MapUtils.isNotEmpty(formAttributeObj)) {
                    JSONObject formAttribute = new JSONObject();
                    formAttribute.put("key", formAttributeObj.getString("key"));
                    formAttribute.put("value", formAttributeObj.getString("value"));
                    formAttributeArray.add(formAttribute);
                }
            }
            resultObj.put("formAttributeList", formAttributeArray);
        }
        resultObj.put("autoexecConfig", autoexecObj);

        /* 异常处理人 **/
        JSONObject workerPolicyObj = new JSONObject();
        JSONObject workerPolicyConfig = configObj.getJSONObject("workerPolicyConfig");
        if (MapUtils.isNotEmpty(workerPolicyConfig)) {
            String defaultWorker = workerPolicyConfig.getString("defaultWorker");
            workerPolicyObj.put("defaultWorker", defaultWorker);
        }
        resultObj.put("workerPolicyConfig", workerPolicyObj);

        JSONArray tagList = configObj.getJSONArray("tagList");
        if (tagList == null) {
            tagList = new JSONArray();
        }
        resultObj.put("tagList", tagList);
        return resultObj;
    }
}
