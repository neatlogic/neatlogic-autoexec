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

package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseRunnerVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseRunnerNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerConnectRefusedException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/5/11 12:18
 **/
@Service
public class AutoexecJobPhaseReFireHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.REFIRE_PHASE.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getExecutePhase();
        jobVo.setIsFirstFire(0);
        if (Objects.equals(jobVo.getAction(), JobAction.RESET_REFIRE.getValue())) {
            resetPhase(jobVo);
            autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(Collections.singletonList(jobVo.getExecutePhase().getId()), JobPhaseStatus.WAITING.getValue());
            autoexecJobService.refreshJobPhaseNodeList(jobVo.getId(), Collections.singletonList(jobVo.getExecutePhase()));
            List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobVo.getId(), Collections.singletonList(jobPhaseVo.getId()));
            for (AutoexecJobPhaseRunnerVo jobPhaseRunnerVo : jobPhaseRunnerVos) {
                autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(jobPhaseVo.getId()), jobPhaseRunnerVo.getRunnerMapId(), JobPhaseStatus.PENDING.getValue());
            }
        }
        if (Objects.equals(jobVo.getAction(), JobAction.REFIRE.getValue())) {
            List<AutoexecJobPhaseNodeVo> needResetNodeList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatus(jobPhaseVo.getId(), Arrays.asList(JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.INVALID.getValue()));
            if (CollectionUtils.isNotEmpty(needResetNodeList)) {
                jobVo.setExecuteJobNodeVoList(needResetNodeList);
                List<RunnerMapVo> runnerMapVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobPhaseVo.getJobId(), Collections.singletonList(jobPhaseVo.getId()));
                if (CollectionUtils.isEmpty(runnerMapVos)) {
                    throw new AutoexecJobPhaseRunnerNotFoundException(jobPhaseVo.getJobId(), jobPhaseVo.getName(), jobPhaseVo.getId());
                }
                //调runner api 重置节点
                autoexecJobMapper.updateJobPhaseRunnerStatusByPhaseIdAndExceptStatus(jobPhaseVo.getId(),JobPhaseStatus.PENDING.getValue(), Collections.singletonList(JobPhaseStatus.COMPLETED.getValue()));
                autoexecJobMapper.updateJobPhaseNodeListStatusByPhaseIdAndExceptStatus(jobPhaseVo.getId(), Arrays.asList(JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.INVALID.getValue()), JobNodeStatus.PENDING.getValue());
                autoexecJobService.updateJobNodeStatus(runnerMapVos, jobVo, JobNodeStatus.PENDING.getValue());
                jobVo.setExecuteJobNodeVoList(null);
            }
        }
        Integer pendingCount = autoexecJobMapper.isHasPendingNode(jobPhaseVo.getId());
        if (pendingCount == null) {
            autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(Collections.singletonList(jobPhaseVo.getId()), JobPhaseStatus.COMPLETED.getValue());
            return null;
        }
        jobPhaseVo.setJobGroupVo(autoexecJobMapper.getJobGroupById(jobPhaseVo.getGroupId()));
        autoexecJobService.executeGroup(jobVo);
        return null;
    }

    /**
     * 重置runner autoexec 作业阶段
     *
     * @param jobVo 作业
     */
    private void resetPhase(AutoexecJobVo jobVo) {
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("phaseName", jobVo.getExecutePhase().getName());
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), Collections.singletonList(jobVo.getExecutePhase().getId()));
        if (CollectionUtils.isEmpty(runnerVos)) {
            throw new AutoexecJobPhaseRunnerNotFoundException(jobVo.getExecutePhase().getName());
        }
        autoexecJobService.checkRunnerHealth(runnerVos);
        for (RunnerMapVo runner : runnerVos) {
            String url = runner.getUrl() + "api/rest/job/phase/reset";
            paramJson.put("passThroughEnv", new JSONObject() {{
                put("runnerId", runner.getRunnerMapId());
            }});

            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(Config.RUNNER_CONNECT_TIMEOUT()).setReadTimeout(Config.RUNNER_READ_TIMEOUT()).sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                throw new RunnerConnectRefusedException(url);
            }
            JSONObject resultJson = requestUtil.getResultJson();
            if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
        }

    }
}
