/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseRunnerVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobPhaseStatusApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

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
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + phaseName);
        }
        //将succeed转成completed，前端不区分成功还是完成
        if (Objects.equals(phaseRunnerStatus, JobNodeStatus.SUCCEED.getValue())) {
            phaseRunnerStatus = JobPhaseStatus.COMPLETED.getValue();
        }
        //更新phase_runner 状态
        autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(jobPhaseVo.getId()), runnerId, phaseRunnerStatus, phaseRunnerWarnCount);
        //更新job 和 phase 状态
        updateJobPhaseStatus(jobVo,jobPhaseVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/status/update";
    }


    private void updateJobPhaseStatus(AutoexecJobVo jobVo,AutoexecJobPhaseVo jobPhaseVo) {
        int warnCount = 0;
        String finalJobPhaseStatus;
        Map<String, Integer> statusCountMap = new HashMap<>();
        statusCountMap.put(JobPhaseStatus.RUNNING.getValue(), 0);
        statusCountMap.put(JobPhaseStatus.FAILED.getValue(), 0);
        statusCountMap.put(JobPhaseStatus.ABORTED.getValue(), 0);
        statusCountMap.put(JobPhaseStatus.COMPLETED.getValue(), 0);
        statusCountMap.put(JobPhaseStatus.PENDING.getValue(), 0);
        statusCountMap.put(JobPhaseStatus.WAIT_INPUT.getValue(), 0);
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
        } else {
            finalJobPhaseStatus = JobPhaseStatus.PENDING.getValue();
        }
        autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseVo.getId(), finalJobPhaseStatus, warnCount));
        //autoexec是不会回调failed的作业状态，故如果存在失败的phase 则更新作业状态为failed
        if(Objects.equals(finalJobPhaseStatus,JobPhaseStatus.FAILED.getValue())){
            jobVo.setStatus(JobStatus.FAILED.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
        }else if (Objects.equals(finalJobPhaseStatus, JobPhaseStatus.COMPLETED.getValue())) {
            //判断所有phase 是否都已跑完（completed），如果是则需要更新job状态
            List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId());
            if (jobPhaseVoList.stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.COMPLETED.getValue()))) {
                jobVo.setStatus(JobPhaseStatus.COMPLETED.getValue());
                autoexecJobMapper.updateJobStatus(jobVo);
            }
        }
    }
}
