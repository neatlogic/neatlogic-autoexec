/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/10/24 12:18
 **/
@Service
public class AutoexecJobPhaseIgnoreHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return JobAction.IGNORE_PHASE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        //目前仅支持忽略 runner阶段
        if (Objects.equals(jobPhaseVo.getExecMode(), ExecMode.RUNNER.getValue())) {
            jobPhaseVo.setStatus(JobPhaseStatus.IGNORED.getValue());
            autoexecJobMapper.updateJobPhaseStatus(jobPhaseVo);
            List<RunnerMapVo> runnerMapVos = autoexecJobMapper.getJobPhaseNodeRunnerListByJobPhaseId(jobPhaseVo.getId());
            if(CollectionUtils.isEmpty(runnerMapVos)){
                throw new AutoexecJobPhaseRunnerNotFoundException(jobPhaseVo);
            }
            autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(jobPhaseVo.getId()),runnerMapVos.get(0).getRunnerMapId(),JobPhaseStatus.IGNORED.getValue());
            autoexecJobMapper.updateJobPhaseNodeStatusByJobPhaseIdAndIsDelete(jobPhaseVo.getId(), JobPhaseStatus.IGNORED.getValue(), 0);
            JSONObject paramJson = new JSONObject();
            paramJson.put("jobId", jobVo.getId());
            paramJson.put("tenant", TenantContext.get().getTenantUuid());
            paramJson.put("execUser", UserContext.get().getUserUuid(true));
            paramJson.put("phaseName", jobPhaseVo.getName());
            paramJson.put("execMode", ExecMode.RUNNER.getValue());
            paramJson.put("phaseNodeList", Collections.singletonList(new AutoexecJobPhaseNodeVo("host",0,0L)));
            for (RunnerMapVo runner : runnerMapVos) {
                String url = runner.getUrl() + "api/rest/job/phase/node/status/ignore";
                HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(5000).setReadTimeout(5000).sendRequest();
                if (StringUtils.isNotBlank(requestUtil.getError())) {
                    throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
                }
                JSONObject resultJson = requestUtil.getResultJson();
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
                }
            }
        }
        return null;
    }
}
