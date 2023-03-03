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

package neatlogic.module.autoexec.api.job.exec;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.asynchronization.threadpool.TransactionSynchronizationPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.AutoexecJobSourceVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseRunnerVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobPhaseStatusApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "回调更新作业阶段状态";
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
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数")
    })
    @Output({
    })
    @Description(desc = "回调更新作业阶段状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phaseName = jsonObj.getString("phase");
        String phaseRunnerStatus = jsonObj.getString("status");
        Integer phaseRunnerWarnCount = jsonObj.getInteger("warnCount");
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        Long runnerId = 0L;
        if (MapUtils.isNotEmpty(passThroughEnv)) {
            if (!passThroughEnv.containsKey("runnerId")) {
                throw new AutoexecJobRunnerNotFoundException("runnerId");
            } else {
                runnerId = passThroughEnv.getLong("runnerId");
            }
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //更新执行用户上下文
        UserVo execUser;
        if(Objects.equals(SystemUser.SYSTEM.getUserUuid(),jobVo.getExecUser())){
            execUser = SystemUser.SYSTEM.getUserVo();
        }else{
            execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        }
        UserContext.init(execUser,"+8:00");

        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseNameWithGroup(jobId, phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + phaseName);
        }
        //将succeed转成completed，前端不区分成功还是完成
        if (Objects.equals(phaseRunnerStatus, JobNodeStatus.SUCCEED.getValue())) {
            phaseRunnerStatus = JobPhaseStatus.COMPLETED.getValue();
        }
        //更新phase_runner 状态,如果status是Completed，则得该runner所有节点或sql都succeed
        boolean isCanUpdatePhaseStatus = true;
        if (Objects.equals(phaseRunnerStatus, JobPhaseStatus.COMPLETED.getValue())) {
            AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
            if (jobSourceVo == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
            isCanUpdatePhaseStatus = autoexecJobSourceActionHandler.getIsCanUpdatePhaseRunner(jobPhaseVo, runnerId);
        }

        if (isCanUpdatePhaseStatus) {
            autoexecJobMapper.updateJobPhaseRunnerStatusAndWarnCount(Collections.singletonList(jobPhaseVo.getId()), runnerId, phaseRunnerStatus, phaseRunnerWarnCount);
        }

        jobVo.setPassThroughEnv(passThroughEnv);
        //更新job 和 phase 状态
        updateJobPhaseStatus(jobVo, jobPhaseVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/status/update";
    }

    void updateJobPhaseStatus(AutoexecJobVo jobVo, AutoexecJobPhaseVo jobPhaseVo) {
        int warnCount = 0;
        String finalJobPhaseStatus = JobPhaseStatus.PENDING.getValue();
        Map<String, Integer> statusCountMap = new HashMap<>();
        for (JobPhaseStatus jobStatus : JobPhaseStatus.values()) {
            statusCountMap.put(jobStatus.getValue(), 0);
        }
        List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobPhaseVo.getJobId(), Collections.singletonList(jobPhaseVo.getId()));
        for (AutoexecJobPhaseRunnerVo jobPhaseRunnerVo : jobPhaseRunnerVos) {
            warnCount += jobPhaseRunnerVo.getWarnCount() == null ? 0 : jobPhaseRunnerVo.getWarnCount();
            statusCountMap.put(jobPhaseRunnerVo.getStatus(), statusCountMap.get(jobPhaseRunnerVo.getStatus()) + 1);
        }

        if (statusCountMap.get(JobPhaseStatus.COMPLETED.getValue()) == jobPhaseRunnerVos.size()) {
            finalJobPhaseStatus = JobPhaseStatus.COMPLETED.getValue();
        } else if (statusCountMap.get(JobPhaseStatus.WAIT_INPUT.getValue()) > 0) {
            finalJobPhaseStatus = JobPhaseStatus.WAIT_INPUT.getValue();
        } else if (statusCountMap.get(JobPhaseStatus.RUNNING.getValue()) > 0) {
            finalJobPhaseStatus = JobPhaseStatus.RUNNING.getValue();
        } else if (statusCountMap.get(JobPhaseStatus.FAILED.getValue()) > 0) {
            finalJobPhaseStatus = JobPhaseStatus.FAILED.getValue();
        } else if (statusCountMap.get(JobPhaseStatus.ABORTED.getValue()) > 0) {
            finalJobPhaseStatus = JobPhaseStatus.ABORTED.getValue();
        } else if (statusCountMap.get(JobPhaseStatus.PAUSED.getValue()) > 0) {
            finalJobPhaseStatus = JobPhaseStatus.PAUSED.getValue();
        } else {
            finalJobPhaseStatus = JobPhaseStatus.PENDING.getValue();
        }
        autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseVo.getId(), finalJobPhaseStatus, warnCount));

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

        //如果状态一致或者状态已经是失败，则无需更新状态，防止多次触发callback；
        if (Objects.equals(jobVo.getStatus(), finalJobPhaseStatus) || JobPhaseStatus.FAILED.getValue().equals(jobVo.getStatus())) {
            return;
        }
        //autoexec是不会回调failed的作业状态，故如果存在失败的phase 则更新作业状态为failed
        if (Arrays.asList(JobPhaseStatus.FAILED.getValue(), JobPhaseStatus.WAIT_INPUT.getValue(), JobPhaseStatus.RUNNING.getValue()).contains(finalJobPhaseStatus)) {
            jobVo.setStatus(finalJobPhaseStatus);
            autoexecJobMapper.updateJobStatus(jobVo);
        } else if (Objects.equals(finalJobPhaseStatus, JobPhaseStatus.COMPLETED.getValue())) {
            //判断所有phase 是否都已跑完（completed|ignored），如果是则需要更新job状态
            List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobVo.getId());
            if (jobPhaseVoList.stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.COMPLETED.getValue()) || Objects.equals(o.getStatus(), JobPhaseStatus.IGNORED.getValue()))) {
                jobVo.setStatus(JobPhaseStatus.COMPLETED.getValue());
                autoexecJobMapper.updateJobStatus(jobVo);
            }
        }
    }
}
