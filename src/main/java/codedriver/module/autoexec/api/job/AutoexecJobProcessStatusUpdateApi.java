/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "作业剧本Id", isRequired = true),
            @Param(name = "status", type = ApiParamType.INTEGER, desc = "创建进程状态，1:创建成功 0:创建失败", isRequired = true),
            @Param(name = "errorMsg", type = ApiParamType.STRING, desc = "失败原因，如果失败则需要传改字段"),
    })
    @Output({
    })
    @Description(desc = "回调创建作业剧本进程状态,更新作业状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobPhaseId = jsonObj.getLong("jobPhaseId");
        Integer status = jsonObj.getInteger("status");
        String errorMsg = jsonObj.getString("errorMsg");
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseLockByPhaseId(jobPhaseId);
        if(jobPhaseVo == null){
            throw new AutoexecJobPhaseNotFoundException(jobPhaseId);
        }
        if(JobStatus.LINING.getValue().equalsIgnoreCase(jobPhaseVo.getStatus())){
            String phaseStatus;
            if(status != null && status ==1){
                phaseStatus =JobStatus.RUNNING.getValue();
            }else{
                phaseStatus =JobStatus.FAILED.getValue();
            }
            autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseId,phaseStatus,errorMsg));
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/process/status/update";
    }
}