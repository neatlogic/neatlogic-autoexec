/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class AutoexecJobPhaseOperationLiveDataGetHandler extends AutoexecJobActionHandlerBase {

    @Override
    public String getName() {
        return JobAction.GET_OPERATION_LIVE_DATA.getValue();
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
        paramJson.put("phase", nodeVo.getJobPhaseName());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("runnerUrl", nodeVo.getRunnerUrl());
        paramJson.put("execMode", phaseVo.getExecMode());
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/operation/livedata/get";
        return JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));
    }
}
