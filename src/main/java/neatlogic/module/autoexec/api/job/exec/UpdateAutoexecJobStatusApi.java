/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.job.exec;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
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
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobStatusApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    UserMapper userMapper;

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
        String statusIng = null;
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //更新执行用户上下文
        UserVo execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        UserContext.init(execUser,"+8:00");

        if (Objects.equals(status, JobStatus.ABORTED.getValue())) {
            statusIng = JobStatus.ABORTING.getValue();
        } else if (Objects.equals(status, JobStatus.PAUSED.getValue())) {
            statusIng = JobStatus.PAUSING.getValue();
        }

        if (StringUtils.isNotBlank(statusIng)) {
            //if(Objects.equals(statusIng, jobVo.getStatus())) {
            jobVo.setStatus(status);
            if (!jsonObj.containsKey("passThroughEnv")) {
                throw new ParamIrregularException("passThroughEnv");
            }
            JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
            if (!passThroughEnv.containsKey("runnerId")) {
                throw new ParamIrregularException("runnerId");
            }
            Long runnerId = passThroughEnv.getLong("runnerId");
            jobVo.setPassThroughEnv(passThroughEnv);
            //update job phase runner
            autoexecJobMapper.updateJobPhaseRunnerStatusByJobIdAndRunnerIdAndStatus(jobId, runnerId, status);
            //如果该job runner 没有一个aborting|pausing phase 则更新为 aborted|paused
            int statusIngCount = autoexecJobMapper.getJobPhaseRunnerCountByJobIdAndRunnerStatus(jobId, statusIng);
            if (statusIngCount == 0) {
                jobVo.setStatus(status);
                autoexecJobMapper.updateJobStatus(jobVo);
                //将中止中和暂停中的phase 状态更新为 已中止和已暂停
                autoexecJobMapper.updateJobPhaseStatusByJobIdAndPhaseStatus(jobId, statusIng, status);
            }
            //}
        } else {
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
