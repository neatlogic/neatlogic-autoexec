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
public class AutoexecJobStatusUpdateApi extends PrivateApiComponentBase {
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
            @Param(name = "jobPhaseUk", type = ApiParamType.STRING, desc = "作业剧本Uk", isRequired = true),
            @Param(name = "node", type = ApiParamType.JSONOBJECT, desc = "执行完的节点"),
            @Param(name = "status", type = ApiParamType.INTEGER, desc = "创建进程状态，1:创建成功 0:创建失败", isRequired = true),
            @Param(name = "errorMsg", type = ApiParamType.STRING, desc = "失败原因，如果失败则需要传改字段"),
    })
    @Output({
    })
    @Description(desc = "回调创建作业剧本进程状态,更新作业状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String jobPhaseName = jsonObj.getString("jobPhaseName");
        JSONObject node = jsonObj.getJSONObject("node");
        String status = jsonObj.getInteger("status") == 1 ? JobStatus.SUCCEED.getValue() : JobStatus.FAILED.getValue();
        String errorMsg = jsonObj.getString("errorMsg");

        if (node == null) {//跟新剧本状态
            AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseLockByJobIdAndPhaseName(jobId, jobPhaseName);
            if (jobPhaseVo == null) {
                throw new AutoexecJobPhaseNotFoundException(jobPhaseName);
            }
            autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseVo.getId(), status, errorMsg));
        } else {//跟新节点状态
            //TODO
        }

        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/status/update";
    }
}
