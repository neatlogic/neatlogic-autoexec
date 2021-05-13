/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.callback;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobProcessStatusUpdateApi extends PublicApiComponentBase {
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
            @Param(name = "jobPhaseName", type = ApiParamType.STRING, desc = "作业剧本Name(唯一标识)", isRequired = true),
            @Param(name = "status", type = ApiParamType.INTEGER, desc = "创建进程状态，1:创建成功 0:创建失败", isRequired = true),
            @Param(name = "errorMsg", type = ApiParamType.STRING, desc = "失败原因，如果失败则需要传改字段"),
            @Param(name = "command", type = ApiParamType.JSONOBJECT, desc = "执行的命令"),
    })
    @Output({
    })
    @Description(desc = "回调创建作业剧本进程状态,更新作业状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String jobPhaseName = jsonObj.getString("jobPhaseName");
        Integer status = jsonObj.getInteger("status");
        String jobAction = jsonObj.getJSONObject("command").getString("action");
        String errorMsg = jsonObj.getString("errorMsg");
        String phaseStatus = null;
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseLockByJobIdAndPhaseName(jobId, jobPhaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobPhaseName);
        }
        if (status != null && status == 1) {
            if (JobAction.EXEC.getValue().equalsIgnoreCase(jobAction)) {
                phaseStatus = JobPhaseStatus.RUNNING.getValue();
            }else if(JobAction.PAUSE.getValue().equalsIgnoreCase(jobAction)) {
                phaseStatus = JobPhaseStatus.PAUSING.getValue();
            }else if(JobAction.STOP.getValue().equalsIgnoreCase(jobAction)) {
                phaseStatus = JobPhaseStatus.STOPPING.getValue();
            }
        }else {
            phaseStatus = JobStatus.FAILED.getValue();
        }
        autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseVo.getId(), phaseStatus, errorMsg));
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/process/status/update";
    }
}
