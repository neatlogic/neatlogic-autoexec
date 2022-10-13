/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.exception.runner.RunnerConnectRefusedException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dto.RestVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.RestUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobAbortHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobAbortHandler.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.ABORT.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck(){
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        //更新job状态 为中止中
        jobVo.setStatus(JobPhaseStatus.ABORTING.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
        //更新phase状态 为中止中
        jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()));
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            if (Arrays.asList(JobPhaseStatus.RUNNING.getValue(),JobPhaseStatus.WAITING.getValue(),JobPhaseStatus.WAIT_INPUT.getValue()).contains(jobPhase.getStatus())) {
                jobPhase.setStatus(JobStatus.ABORTING.getValue());
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
                autoexecJobMapper.updateBatchJobPhaseRunnerStatus(jobPhase.getId(), JobPhaseStatus.ABORTING.getValue());
            }
        }
        //更新node状态 为中止中
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndNodeStatusList(jobVo.getId(), Collections.singletonList(JobNodeStatus.RUNNING.getValue()));
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            nodeVo.setStatus(JobNodeStatus.ABORTING.getValue());
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }

        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdListAndStatus(jobVo.getId(), jobVo.getPhaseIdList(),JobNodeStatus.ABORTING.getValue());
        if (CollectionUtils.isEmpty(runnerVos)) {
            jobVo.setStatus(JobStatus.ABORTED.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
        }else {
            runnerVos = runnerVos.stream().filter(o->StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>( Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
            autoexecJobService.checkRunnerHealth(runnerVos);
            JSONObject paramJson = new JSONObject();
            paramJson.put("jobId", jobVo.getId());
            paramJson.put("tenant", TenantContext.get().getTenantUuid());
            paramJson.put("execUser", UserContext.get().getUserUuid(true));
            String result = StringUtils.EMPTY;
            String url = null;
            try {
                for (RunnerMapVo runner : runnerVos) {
                    paramJson.put("passThroughEnv", new JSONObject() {{
                        put("runnerId", runner.getRunnerMapId());
                        //put("phaseSort", jobVo.getCurrentGroupSort());
                    }});
                    url = runner.getUrl() + "api/rest/job/abort";
                    RestVo restVo = new RestVo.Builder(url, AuthenticateType.BUILDIN.getValue()).setPayload(paramJson).build();
                    result = RestUtil.sendPostRequest(restVo);
                    JSONObject resultJson = JSONObject.parseObject(result);
                    if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                        throw new RunnerHttpRequestException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new RunnerConnectRefusedException(url + " " + result);
            }
        }
        return null;
    }
}
