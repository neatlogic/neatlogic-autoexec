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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.TransactionSynchronizationPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.config.AutoexecConfig;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseRunnerVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobPhaseStatusApi extends PrivateApiComponentBase {
    private final Logger logger = LoggerFactory.getLogger(UpdateAutoexecJobPhaseStatusApi.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "nmaaje.updateautoexecjobphasestatusapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    /*
     * 如果status = completed，表示除了“已忽略”的节点，其它节点已成功,将web端phase runner状态更新为completed
     * 如果status = succeed 表示除了“已忽略”的节点，其它节点都已成功,将web端phase runner状态更新为completed
     * 如果status = failed 表示存在“失败中止”节点，将web端phase runner状态更新为failed
     */

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "execId", type = ApiParamType.LONG, desc = "nmaaje.updateautoexecjobphasestatusapi.input.param.desc", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "term.autoexec.phase", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "common.status", isRequired = true),
            @Param(name = "needInform", type = ApiParamType.STRING, desc = "nmaaje.updateautoexecjobphasestatusapi.input.param.needinform", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.passthroughenv")
    })
    @Output({
    })
    @Description(desc = "nmaaje.updateautoexecjobphasestatusapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phaseName = jsonObj.getString("phase");
        String phaseRunnerStatusParam = jsonObj.getString("status");
        Integer phaseRunnerWarnCount = jsonObj.getInteger("warnCount");
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        Integer needInform = jsonObj.getInteger("needInform");
        Integer isFirstFire = 0;
        Integer isPartialNodeOrSqlRun = 0;
        Long runnerId = 0L;
        String phaseRunnerStatus = phaseRunnerStatusParam;
        if (MapUtils.isNotEmpty(passThroughEnv)) {
            if (!passThroughEnv.containsKey("runnerId")) {
                throw new AutoexecJobRunnerNotFoundException("runnerId");
            } else {
                runnerId = passThroughEnv.getLong("runnerId");
            }
            if (passThroughEnv.containsKey("isFirstFire")) {
                isFirstFire = passThroughEnv.getInteger("isFirstFire");
            }
            if (passThroughEnv.containsKey("isPartialNodeOrSqlRun")) {
                isPartialNodeOrSqlRun = passThroughEnv.getInteger("isPartialNodeOrSqlRun");
            }
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        jobVo.setIsFirstFire(isFirstFire);
        jobVo.setActionParam(jsonObj);
        //更新执行用户上下文
        autoexecJobActionService.initExecuteUserContext(jobVo, passThroughEnv);

        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseNameWithGroup(jobId, phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + phaseName);
        }

        jobVo.setPassThroughEnv(passThroughEnv);
        //phase 状态
        updateJobPhaseStatus(jobVo, jobPhaseVo, isPartialNodeOrSqlRun, phaseRunnerStatus, runnerId, phaseRunnerWarnCount);

        //informGlobalFail
        if (needInform == 1 && Arrays.asList(JobPhaseStatus.FAILED.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue()).contains(phaseRunnerStatusParam)) {
            informGlobalFail(jobVo, jobPhaseVo, phaseRunnerStatusParam, runnerId);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/status/update";
    }

    void updateJobPhaseStatus(AutoexecJobVo jobVo, AutoexecJobPhaseVo jobPhaseVo, int isPartialNodeOrSqlRun, String currentPhaseStatus, Long runnerId, Integer phaseRunnerWarnCount) {
        List<String> statusList;
        String finalJobPhaseStatus;
        if (isPartialNodeOrSqlRun == 1) {
            autoexecJobService.updatePartialNodeJobAndPhaseWithRunnerId(jobPhaseVo, runnerId, jobVo, currentPhaseStatus, phaseRunnerWarnCount);
        } else {
            autoexecJobMapper.updateJobPhaseRunnerStatusAndWarnCount(jobPhaseVo.getId(), runnerId, currentPhaseStatus, phaseRunnerWarnCount);

        }
        List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobPhaseVo.getJobId(), Collections.singletonList(jobPhaseVo.getId()));
        statusList = jobPhaseRunnerVos.stream().map(AutoexecJobPhaseRunnerVo::getStatus).collect(toList());
        finalJobPhaseStatus = autoexecJobService.getJobPhaseStatus(statusList, currentPhaseStatus);
        autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseVo.getId(), finalJobPhaseStatus, phaseRunnerWarnCount, jobPhaseVo.getStartTime()));

        //如果最终阶段状态是wait_input则更新作业状态为waitInput
        if (Objects.equals(finalJobPhaseStatus, JobPhaseStatus.WAIT_INPUT.getValue())) {
            AutoexecJobVo job = new AutoexecJobVo();
            job.setId(jobVo.getId());
            job.setStatus(JobStatus.WAIT_INPUT.getValue());
            autoexecJobMapper.updateJobStatus(job);
        }

        //如果阶段需要更新别的阶段的执行目标 且 状态为complete
        if (jobPhaseVo.getIsPreOutputUpdateNode() == 1 && Objects.equals(JobPhaseStatus.COMPLETED.getValue(), finalJobPhaseStatus)) {
            try {
                autoexecJobService.updateNodeByPreOutput(jobVo, jobPhaseVo);
            } catch (ApiRuntimeException ex) {
                //如果根据上游参数初始化执行目标失败，上游出参的值不存在或不合法，则更新phase和job 状态为已失败
                TransactionSynchronizationPool.executeAfterRollback(new NeatLogicThread("AUTOEXEC-JOB-PHASE-ERROR-UPDATE") {
                    @Override
                    protected void execute() {
                        autoexecJobService.updatePhaseJobStatus2Failed(jobVo, jobPhaseVo);
                    }
                });
                throw new ApiRuntimeException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * 失败阶段时inform所有autoexec
     *
     * @param phaseVo 阶段
     */
    private void informGlobalFail(AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo, String phaseRunnerStatusParam, Long runnerId) {
        JSONObject jsonObj = jobVo.getActionParam();
        JSONObject informParam = new JSONObject();
        informParam.put("action", "informGlobalFail");
        informParam.put("originRunnerId", runnerId);
        informParam.put("phaseName", phaseVo.getName());
        informParam.put("phaseStatus", phaseRunnerStatusParam);
        jsonObj.put("informParam", informParam);
        jsonObj.put("socketFileName", "job" + jsonObj.getString("execId"));
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobRunnerListByJobIdAndGroupId(phaseVo.getJobId(), phaseVo.getGroupId());
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        autoexecJobService.checkRunnerHealth(runnerVos);
        for (RunnerMapVo runnerVo : runnerVos) {
            String url = String.format("%s/api/rest/job/phase/socket/write", runnerVo.getUrl());
            HttpRequestUtil request = HttpRequestUtil.post(url)
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT())
                    .sendRequest();
            if (request.getResponseCode() != 200) {
                String errMsg = String.format("test account failed, ResponseCode:%d, ErrorMsg: %s, Exception: %s, Result: %s", request.getResponseCode(), request.getErrorMsg(), request.getError(), request.getResult());
                throw new ApiRuntimeException(errMsg);
            }
            String error = request.getError();
            if (StringUtils.isNotBlank(error)) {
                throw new RunnerHttpRequestException(url + ":" + error);
            }
        }
    }
}
