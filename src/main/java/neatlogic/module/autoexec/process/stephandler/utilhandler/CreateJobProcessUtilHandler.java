/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.process.stephandler.utilhandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.core.INotifyPolicyHandler;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.crossover.IProcessTaskStepDataCrossoverMapper;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import neatlogic.module.autoexec.notify.handler.AutoexecNotifyPolicyHandler;
import neatlogic.module.autoexec.process.constvalue.CreateJobProcessStepHandlerType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author linbq
 * @since 2021/9/2 14:30
 **/
@Service
public class CreateJobProcessUtilHandler extends ProcessStepInternalHandlerBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getHandler() {
        return CreateJobProcessStepHandlerType.CREATE_JOB.getHandler();
    }

    @Override
    public Object getStartStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return getNonStartStepInfo(currentProcessTaskStepVo);
    }

    @Override
    public Object getNonStartStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        JSONObject resultObj = new JSONObject();
        List<Long> jobIdList = autoexecJobMapper.getJobIdListByInvokeId(currentProcessTaskStepVo.getId());
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


    /**
     * 返回步骤动作，校验时用
     */
    @Override
    public IOperationType[] getStepActions() {
        return new IOperationType[]{
                ProcessTaskStepOperationType.STEP_VIEW,
                ProcessTaskStepOperationType.STEP_TRANSFER
        };
    }

    /**
     * 返回步骤按钮列表
     */
    @Override
    public IOperationType[] getStepButtons() {
        return new IOperationType[]{
                ProcessTaskStepOperationType.STEP_COMPLETE,
                ProcessTaskStepOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskStepOperationType.STEP_ACCEPT
        };
    }

    @Override
    public Class<? extends INotifyPolicyHandler> getNotifyPolicyHandlerClass() {
        return AutoexecNotifyPolicyHandler.class;
    }

    @Override
    public String[] getRegulateKeyList() {
        return new String[]{"enableAuthority", "authorityList", "notifyPolicyConfig", "actionConfig", "customButtonList", "customStatusList", "replaceableTextList", "createJobConfig", "workerPolicyConfig", "tagList", "formSceneUuid", "formSceneName", "isAllowProcessOnMobile"};
    }
}
