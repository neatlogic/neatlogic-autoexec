/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectRefusedException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public boolean myValidate(AutoexecJobVo jobVo) {
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getExecuteJobPhaseList().get(0);
        jobVo.setStatus(JobStatus.RUNNING.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
        jobPhaseVo.setStatus(JobPhaseStatus.RUNNING.getValue());
        autoexecJobMapper.updateJobPhaseStatus(jobPhaseVo);
        //如果是sqlfile类型的phase 需额外清除状态
        if (Objects.equals(ExecMode.SQL.getValue(), jobPhaseVo.getExecMode()) && Objects.equals(jobVo.getActionParam().getInteger("isAll"), 1)) {
            autoexecJobService.resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameList(jobVo.getId(), Collections.singletonList(jobPhaseVo.getName()));
        }
        if (Objects.equals(jobVo.getAction(), JobAction.RESET_REFIRE.getValue())) {
            resetPhase(jobVo);
            autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(jobVo.getExecuteJobPhaseList().stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()), JobPhaseStatus.PENDING.getValue());
            autoexecJobService.refreshJobPhaseNodeList(jobVo.getId(), jobVo.getExecuteJobPhaseList());
        }
        if (Objects.equals(jobVo.getAction(), JobAction.REFIRE.getValue())) {
            List<AutoexecJobPhaseNodeVo> needResetNodeList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatus(jobVo.getId(), jobPhaseVo.getId(), Arrays.asList(JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue()));
            autoexecJobMapper.updateJobPhaseNodeListStatus(needResetNodeList.stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());
            resetJobNodeStatus(jobVo, needResetNodeList);
        }
        jobPhaseVo.setJobGroupVo(autoexecJobMapper.getJobGroupById(jobPhaseVo.getGroupId()));
        executeGroup(jobVo);
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
        paramJson.put("phaseName", jobVo.getExecuteJobPhaseList().get(0).getName());
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), jobVo.getExecuteJobPhaseList().stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(runnerVos)) {
            throw new AutoexecJobRunnerNotFoundException(jobVo.getExecuteJobPhaseList().stream().map(AutoexecJobPhaseVo::getName).collect(Collectors.toList()));
        }
        checkRunnerHealth(runnerVos);
        for (RunnerMapVo runner : runnerVos) {
            String url = runner.getUrl() + "api/rest/job/phase/reset";
            paramJson.put("passThroughEnv", new JSONObject() {{
                put("runnerId", runner.getRunnerMapId());
            }});

            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setConnectTimeout(5000).setReadTimeout(5000).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                throw new AutoexecJobRunnerConnectRefusedException(url);
            }
            JSONObject resultJson = requestUtil.getResultJson();
            if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                throw new AutoexecJobRunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
        }

    }
}
