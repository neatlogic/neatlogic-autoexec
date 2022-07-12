/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobStatusApi extends PrivateApiComponentBase {
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
    @Description(desc = "回调更新作业状态,目前只有 中止和暂停会调该接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String status = jsonObj.getString("status");
        String statusIng;
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        jobVo.setStatus(status);
        if(Objects.equals(status,JobStatus.ABORTED.getValue())){
            statusIng = JobStatus.ABORTING.getValue();
        }else if(Objects.equals(status,JobStatus.PAUSED.getValue())){
            statusIng = JobStatus.PAUSING.getValue();
        }else{
            autoexecJobMapper.updateJobStatus(jobVo);
            return null;
        }

        //如果该job 没有一个aborting|pausing phase 则更新为 aborted|paused
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
