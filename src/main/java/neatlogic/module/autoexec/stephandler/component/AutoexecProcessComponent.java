/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.process.constvalue.*;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
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
        if (StringUtils.isNotBlank(config)) {
            JSONObject autoexecConfig = (JSONObject) JSONPath.read(config, "autoexecConfig");
            if (MapUtils.isNotEmpty(autoexecConfig)) {
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
                    System.out.println(executeConfig.toJSONString());
                    jobVo.setExecuteConfig(executeConfig.toJavaObject(AutoexecCombopExecuteConfigVo.class));
                }
                Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
                //如果工单步骤ID没有绑定自动化作业ID，则需要创建自动化作业
                if (autoexecJobId == null) {
                    jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
                    jobVo.setRoundCount(32);
                    jobVo.setOperationId(combopId);
                    jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
                    jobVo.setInvokeId(currentProcessTaskStepVo.getId());
                    jobVo.setRouteId(currentProcessTaskStepVo.getProcessStepUuid());
                    jobVo.setIsFirstFire(1);
                    jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
                    try {
                        autoexecJobActionService.validateCreateJob(jobVo);
//                        AutoexecJobVo jobVo = autoexecJobActionService.validateCreateJobFromCombop(paramObj, false);
//                        autoexecJobActionService.fire(jobVo);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        String failPolicy = autoexecConfig.getString("failPolicy");
                        if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                            processTaskStepComplete(currentProcessTaskStepVo.getId(), null);
                        }
                        /* 异常提醒 **/
//                        IProcessStepHandlerUtil.saveStepRemind(currentProcessTaskStepVo, currentProcessTaskStepVo.getId(), "创建作业失败", ProcessTaskStepRemindType.ERROR);
//                        throw new ProcessTaskException(e);
                    }
                } else {//否则重新刷新作业运行参数，以及节点后，需人工干预重跑作业
                    autoexecJobService.refreshJobParam(autoexecJobId, param);
                    autoexecJobService.refreshJobNodeList(autoexecJobId, executeConfig);
                }
            }
        }
        return 1;
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
