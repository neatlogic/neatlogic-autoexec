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

package neatlogic.module.autoexec.process.stephandler.component;

import com.alibaba.fastjson.*;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.AutoexecNotifyTriggerType;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobEnvVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecServiceConfigExpiredException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.process.constvalue.*;
import neatlogic.framework.process.crossover.*;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import neatlogic.framework.process.stephandler.core.IProcessStepHandler;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.process.stephandler.core.ProcessStepThread;
import neatlogic.module.autoexec.constvalue.FailPolicy;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.process.constvalue.CreateJobProcessStepHandlerType;
import neatlogic.module.autoexec.process.dto.AutoexecJobBuilder;
import neatlogic.module.autoexec.process.dto.CreateJobConfigConfigVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigVo;
import neatlogic.module.autoexec.process.util.CreateJobConfigUtil;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import neatlogic.module.autoexec.service.AutoexecServiceService;
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
public class CreateJobProcessComponent extends ProcessStepHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(CreateJobProcessComponent.class);
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Resource
    private AutoexecServiceService autoexecServiceService;


    @Override
    public String getHandler() {
        return CreateJobProcessStepHandlerType.CREATE_JOB.getHandler();
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
        return CreateJobProcessStepHandlerType.CREATE_JOB.getType();
    }

    @Override
    public ProcessStepMode getMode() {
        return ProcessStepMode.MT;
    }

    @Override
    public String getName() {
        return CreateJobProcessStepHandlerType.CREATE_JOB.getName();
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
            if (StringUtils.isBlank(config)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId());
                return 0;
            }
            JSONObject createJobConfig = (JSONObject) JSONPath.read(config, "createJobConfig");
            if (MapUtils.isEmpty(createJobConfig)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId());
                return 0;
            }
            CreateJobConfigVo createJobConfigVo = createJobConfig.toJavaObject(CreateJobConfigVo.class);
            // rerunStepToCreateNewJob为1时表示重新激活自动化步骤时创建新作业，rerunStepToCreateNewJob为0时表示重新激活自动化步骤时不创建新作业，也不重跑旧作业，即什么都不做
            Integer rerunStepToCreateNewJob = createJobConfigVo.getRerunStepToCreateNewJob();
            if (!Objects.equals(rerunStepToCreateNewJob, 1)) {
                Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
                if (autoexecJobId != null) {
                    processTaskStepComplete(currentProcessTaskStepVo.getId());
                    return 1;
                }
            }
            autoexecJobMapper.deleteAutoexecJobByProcessTaskStepId(currentProcessTaskStepVo.getId());
            // 删除上次创建作业的报错信息
            ProcessTaskStepDataVo processTaskStepData = new ProcessTaskStepDataVo();
            processTaskStepData.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
            processTaskStepData.setProcessTaskStepId(currentProcessTaskStepVo.getId());
            processTaskStepData.setType("autoexecCreateJobError");
            processTaskStepData = processTaskStepDataCrossoverMapper.getProcessTaskStepData(processTaskStepData);
            if (processTaskStepData != null) {
                processTaskStepDataCrossoverMapper.deleteProcessTaskStepData(processTaskStepData);
            }
            List<CreateJobConfigConfigVo> configList = createJobConfigVo.getConfigList();
            if (CollectionUtils.isEmpty(configList)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId());
                return 0;
            }
            List<AutoexecJobBuilder> builderList = new ArrayList<>();
            for (CreateJobConfigConfigVo createJobConfigConfigVo : configList) {
                if (createJobConfigConfigVo == null) {
                    continue;
                }
                if (Objects.equals(createJobConfigConfigVo.getType(), "service")) {
                    JSONObject paramObj = null;
                    Long processTaskId = currentProcessTaskStepVo.getProcessTaskId();
                    // 如果工单有表单信息，则查询出表单配置及数据
                    IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
                    List<FormAttributeVo> formAttributeList = processTaskCrossoverService.getFormAttributeListByProcessTaskIdAngTagNew(processTaskId, createJobConfigConfigVo.getFormTag());
                    if (CollectionUtils.isNotEmpty(formAttributeList)) {
                        List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskIdAndTagNew(processTaskId, createJobConfigConfigVo.getFormTag());
                        for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                            if (Objects.equals(attributeDataVo.getAttributeUuid(), createJobConfigConfigVo.getFormAttributeUuid())) {
                                paramObj = (JSONObject) attributeDataVo.getDataObj();
                                break;
                            }
                        }
                    }
                    if (MapUtils.isNotEmpty(paramObj)) {
                        Long serviceId = paramObj.getLong("serviceId");
                        AutoexecServiceVo autoexecServiceVo = autoexecServiceMapper.getAutoexecServiceById(serviceId);
                        if (autoexecServiceVo == null) {
                            throw new AutoexecServiceNotFoundException(serviceId);
                        }
                        if (Objects.equals(autoexecServiceVo.getConfigExpired(), 1)) {
                            throw new AutoexecServiceConfigExpiredException(autoexecServiceVo.getName());
                        }
                        Long combopId = autoexecServiceVo.getCombopId();
                        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(combopId);
                        if (autoexecCombopVersionVo == null) {
                            throw new AutoexecCombopActiveVersionNotFoundException(combopId);
                        }
                        String name = paramObj.getString("name");
                        Long scenarioId = paramObj.getLong("scenarioId");
                        JSONArray formAttributeDataList = paramObj.getJSONArray("formAttributeDataList");
                        JSONArray hidecomponentList = paramObj.getJSONArray("hidecomponentList");
                        Integer roundCount = paramObj.getInteger("roundCount");
                        String executeUser = paramObj.getString("executeUser");
                        Long protocol = paramObj.getLong("protocol");
                        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = paramObj.getObject("executeNodeConfig", AutoexecCombopExecuteNodeConfigVo.class);
                        JSONObject runtimeParamMap = paramObj.getJSONObject("runtimeParamMap");
                        AutoexecJobBuilder autoexecJobBuilder = autoexecServiceService.getAutoexecJobBuilder(autoexecServiceVo, autoexecCombopVersionVo, name, scenarioId, formAttributeDataList, hidecomponentList, roundCount, executeUser, protocol, executeNodeConfig, runtimeParamMap);
                        if (autoexecJobBuilder != null) {
                            builderList.add(autoexecJobBuilder);
                        }
                    }
                } else {
                    Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(createJobConfigConfigVo.getCombopId());
                    if (activeVersionId == null) {
                        throw new AutoexecCombopActiveVersionNotFoundException(createJobConfigConfigVo.getCombopId());
                    }
                    AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
                    if (autoexecCombopVersionVo == null) {
                        throw new AutoexecCombopVersionNotFoundException(activeVersionId);
                    }
                    // 根据配置信息创建AutoexecJobBuilder对象
                    List<AutoexecJobBuilder> list = CreateJobConfigUtil.createAutoexecJobBuilderList(currentProcessTaskStepVo, createJobConfigConfigVo, autoexecCombopVersionVo);
                    builderList.addAll(list);
                }

            }
            if (CollectionUtils.isEmpty(builderList)) {
                processTaskStepComplete(currentProcessTaskStepVo.getId());
            }
            JSONArray errorMessageList = new JSONArray();
            boolean flag = false;
            List<Long> jobIdList = new ArrayList<>();
            for (AutoexecJobBuilder builder : builderList) {
                AutoexecJobVo jobVo = builder.build();
                jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
                jobVo.setInvokeId(currentProcessTaskStepVo.getId());
                jobVo.setRouteId(currentProcessTaskStepVo.getId().toString());
                jobVo.setSource(AutoExecJobProcessSource.ITSM.getValue());
                jobVo.setIsFirstFire(1);
                jobVo.setAssignExecUser(SystemUser.SYSTEM.getUserUuid());
                try {
                    autoexecJobActionService.validateCreateJob(jobVo);
                    autoexecJobMapper.insertAutoexecJobProcessTaskStep(jobVo.getId(), currentProcessTaskStepVo.getId());
                    jobIdList.add(jobVo.getId());
                } catch (Exception e) {
                    // 增加提醒
                    logger.error(e.getMessage(), e);
                    logger.error(JSON.toJSONString(builder));
                    JSONObject errorMessageObj = new JSONObject();
                    errorMessageObj.put("jobId", jobVo.getId());
                    errorMessageObj.put("jobName", jobVo.getName());
                    errorMessageObj.put("error", e.getMessage() + " jobVo=" + JSON.toJSONString(builder));
                    errorMessageList.add(errorMessageObj);
                    flag = true;
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
                String failPolicy = createJobConfigVo.getFailPolicy();
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
                            processTaskStepComplete(currentProcessTaskStepVo.getId());
                        }
                    } else {
                        processTaskStepComplete(currentProcessTaskStepVo.getId());
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
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ProcessTaskException(e);
        }
        return 1;
    }

    private void processTaskStepComplete(Long processTaskStepId) {
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        List<Long> toProcessTaskStepIdList = processTaskCrossoverMapper.getToProcessTaskStepIdListByFromIdAndType(processTaskStepId, ProcessFlowDirection.FORWARD.getValue());
        if (toProcessTaskStepIdList.size() == 1) {
            Long nextStepId = toProcessTaskStepIdList.get(0);
            try {
                ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
                JSONObject paramObj = processTaskStepVo.getParamObj();
                paramObj.put("nextStepId", nextStepId);
                paramObj.put("action", ProcessTaskStepOperationType.STEP_COMPLETE.getValue());
                /* 自动处理 **/
                IProcessStepHandler handler = this;
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
        JSONObject createJobConfig = (JSONObject) JSONPath.read(config, "createJobConfig");
        if (MapUtils.isEmpty(createJobConfig)) {
            return 0;
        }
        CreateJobConfigVo createJobConfigVo = createJobConfig.toJavaObject(CreateJobConfigVo.class);
        List<CreateJobConfigConfigVo> configList = createJobConfigVo.getConfigList();
//        JSONArray configList = (JSONArray) JSONPath.read(config, "createJobConfig.configList");
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
        for (CreateJobConfigConfigVo createJobConfigConfigVo : configList) {
//            JSONObject configObj = configList.getJSONObject(j);
//            if (MapUtils.isEmpty(configObj)) {
//                continue;
//            }
//            JSONArray formAttributeList = configObj.getJSONArray("formAttributeList");
            JSONArray formAttributeList = createJobConfigConfigVo.getFormAttributeMappingList();
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


        Map<String, FormAttributeVo> formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(FormAttributeVo::getUuid, e -> e));
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
