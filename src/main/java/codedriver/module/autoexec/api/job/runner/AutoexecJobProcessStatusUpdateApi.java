/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.runner;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobProcessStatusUpdateApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "回调创建作业剧本进程状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "status", type = ApiParamType.INTEGER, desc = "创建进程状态，1:创建成功 0:创建失败", isRequired = true),
            @Param(name = "errorMsg", type = ApiParamType.STRING, desc = "失败原因，如果失败则需要传改字段"),
            @Param(name = "command", type = ApiParamType.JSONOBJECT, desc = "执行的命令", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "环境变量", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "回调创建作业剧本进程状态,更新作业状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Integer statusParam = jsonObj.getInteger("status");
        String jobAction = jsonObj.getJSONObject("command").getString("action");
        String errorMsg = jsonObj.getString("errorMsg");
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        Long runnerId = null;
        if (MapUtils.isNotEmpty(passThroughEnv)) {
            if (MapUtils.isNotEmpty(passThroughEnv)) {
                if (!passThroughEnv.containsKey("runnerId")) {
                    throw new AutoexecJobRunnerNotFoundException("runnerId");
                } else {
                    runnerId = passThroughEnv.getLong("runnerId");
                }
            }
        }
        String status = null;
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId.toString());
        }
        List<AutoexecJobPhaseVo> jobPhaseVoList = null;
        if (statusParam != null && statusParam == 1) {
            if (JobAction.ABORT.getValue().equalsIgnoreCase(jobAction)) {
                status = JobPhaseStatus.ABORTED.getValue();
                jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobId, Collections.singletonList(JobPhaseStatus.ABORTING.getValue()));
            }
            /*
             * 1、更新所有该runnerId的autoexec_job_phase_runner的status 为 aborted
             * 2、更新所有该runnerId的autoexec_job_phase_node的status 为aborted
             * 3、更新所有满足所有runner的状态都是aborted的autoexec_job_phase状态 为aborted
             * 4、如果所有autoexec_job_phase都不存在aborting状态，则跟新autoexec_job状态 为aborted
             */
            if (CollectionUtils.isNotEmpty(jobPhaseVoList)) {
                List<Long> jobPhaseIdAbortedList = new ArrayList<>();
                List<Long> jobPhaseIdList = jobPhaseVoList.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList());
                //1
                autoexecJobMapper.updateJobPhaseRunnerStatusBatch(jobPhaseIdList, status, runnerId);
                //2
                List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = autoexecJobMapper.getAutoexecJobNodeListByJobPhaseIdListAndStatusAndRunnerId(jobPhaseIdList, JobPhaseStatus.ABORTING.getValue(), runnerId);
                if (CollectionUtils.isNotEmpty(jobPhaseNodeVoList)) {
                    autoexecJobMapper.updateJobPhaseNodeListStatus(jobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobPhaseStatus.ABORTED.getValue());
                }
                //3
                List<HashMap<String, String>> phaseAbortingCountMapList = autoexecJobMapper.getJobPhaseRunnerAbortingCountMapCountByJobId(jobId);
                HashMap<String, Integer> phaseIdAbortingCountMap = new HashMap<>();
                for (HashMap<String, String> phaseAbortingCountMap : phaseAbortingCountMapList) {
                    phaseIdAbortingCountMap.put(phaseAbortingCountMap.get("job_phase_id"), Integer.valueOf(phaseAbortingCountMap.get("count")));
                }
                for (Long phaseId : jobPhaseIdList) {
                    if (phaseIdAbortingCountMap.get(phaseId.toString()) == 0) {
                        jobPhaseIdAbortedList.add(phaseId);
                    }
                }
                autoexecJobMapper.updateJobPhaseRunnerStatusBatch(jobPhaseIdAbortedList, JobPhaseStatus.ABORTED.getValue(), runnerId);
            }
            if (StringUtils.isNotBlank(status)) {
                //4
                if (autoexecJobMapper.getJobPhaseStatusCountByJobIdAndStatus(jobId, JobPhaseStatus.ABORTING.getValue()) == 0) {
                    autoexecJobMapper.updateJobStatus(new AutoexecJobVo(jobId, status));
                }
            }
        }

        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/process/status/update";
    }
}
