/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobStatusUpdateApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "回调更新作业状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数")
    })
    @Output({
    })
    @Description(desc = "回调更新作业状态,如：已暂停、已中止")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String status = jsonObj.getString("status");
        String statusIng;
        if(Objects.equals(status,JobStatus.ABORTED.getValue())){
            statusIng = JobStatus.ABORTING.getValue();
        }else if(Objects.equals(status,JobStatus.PAUSED.getValue())){
            statusIng = JobStatus.PAUSING.getValue();
        }else{
            statusIng = status;
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        Integer phaseSort = 0;
        if (MapUtils.isEmpty(passThroughEnv)) {
            throw new ParamIrregularException("passThroughEnv");
        }
        if (!Objects.equals(status,JobStatus.ABORTED.getValue()) && !passThroughEnv.containsKey("phaseSort")) {
            throw new ParamIrregularException("phaseSort");
        } else {
            phaseSort = passThroughEnv.getInteger("phaseSort");
        }
        Long runnerId = null;
        if (!passThroughEnv.containsKey("runnerId")) {
            throw new AutoexecJobRunnerNotFoundException("runnerId");
        } else {
            runnerId = passThroughEnv.getLong("runnerId");
        }

        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndSort(jobId,phaseSort);
        List<Long> jobPhaseIdList = jobPhaseVoList.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList());
        autoexecJobMapper.updateJobPhaseRunnerStatus(jobPhaseIdList, runnerId, status);
        //如果该phase 没有一个 aborting|pausing runner 则更新为 aborted|paused
        List<AutoexecJobPhaseVo> jobPhaseAbortingRunnerCountList = autoexecJobMapper.getJobPhaseRunnerCountByJobIdAndStatus(jobVo.getId(),statusIng);
        for (AutoexecJobPhaseVo jobPhase : jobPhaseVoList) {
            if (Objects.equals(jobPhase.getStatus(), statusIng)&& jobPhaseAbortingRunnerCountList.stream().noneMatch(o->Objects.equals(o.getId(),jobPhase.getId()))) {
                jobPhase.setStatus(status);
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
            }
        }
        //如果该job 没有一个aborting|pausing runner 则更新为 aborted|paused
        int statusIngCount = autoexecJobMapper.getJobPhaseStatusCountByJobIdAndStatus(jobVo.getId(), statusIng);
        if(statusIngCount == 0){
            jobVo.setStatus(status);
            autoexecJobMapper.updateJobStatus(jobVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/status/update";
    }
}
