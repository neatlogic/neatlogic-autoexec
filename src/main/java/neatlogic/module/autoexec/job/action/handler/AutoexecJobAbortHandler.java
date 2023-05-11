/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.job.action.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerConnectRefusedException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.dto.RestVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.RestUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
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
        jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobVo.getId()));
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
