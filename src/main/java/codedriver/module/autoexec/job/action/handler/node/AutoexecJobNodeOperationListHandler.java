/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeOperationStatusVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeOperationListHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeOperationListHandler.class);
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.GET_NODE_OPERATION_LIST.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        JSONObject paramJson = jobVo.getActionParam();
        paramJson.put("jobId", phaseVo.getJobId());
        paramJson.put("resourceId", nodeVo.getResourceId());
        paramJson.put("phase", nodeVo.getJobPhaseName());
        paramJson.put("phaseId", nodeVo.getJobPhaseId());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        paramJson.put("runnerUrl", nodeVo.getRunnerUrl());
        paramJson.put("execMode", phaseVo.getExecMode());
        JSONObject result = new JSONObject();
        result.put("isRefresh", 0);
        AutoexecJobPhaseNodeVo phaseNodeVo = autoexecJobService.getNodeOperationStatus(paramJson, true);
        List<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVos = phaseNodeVo.getOperationStatusVoList();
        result.put("operationStatusList", operationStatusVos);
        String nodeStatusOld = paramJson.getString("status");
        if (Objects.equals(nodeStatusOld, JobNodeStatus.RUNNING.getValue()) || Objects.equals(phaseNodeVo.getStatus(), JobNodeStatus.RUNNING.getValue())) {
            result.put("isRefresh", 1);
        }
        return result;
    }
}
