/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
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
 * @since 2022/11/22 12:18
 **/
@Service
public class AutoexecJobPauseHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobPauseHandler.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.PAUSE.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        //更新job状态 为暂停中
        jobVo.setStatus(JobPhaseStatus.PAUSING.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
        //更新phase状态 为暂停中
        jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobVo.getId()));
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            if (Arrays.asList(JobPhaseStatus.RUNNING.getValue(), JobPhaseStatus.WAITING.getValue(), JobPhaseStatus.WAIT_INPUT.getValue()).contains(jobPhase.getStatus())) {
                jobPhase.setStatus(JobStatus.PAUSING.getValue());
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
                autoexecJobMapper.updateBatchJobPhaseRunnerStatus(jobPhase.getId(), JobPhaseStatus.PAUSING.getValue());
            }
        }
        //更新node状态 为暂停中
       /* List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndNodeStatusList(jobVo.getId(), Collections.singletonList(JobNodeStatus.RUNNING.getValue()));
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            nodeVo.setStatus(JobNodeStatus.PAUSING.getValue());
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }*/

        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdListAndStatus(jobVo.getId(), jobVo.getPhaseIdList(), JobNodeStatus.PAUSING.getValue());
        if (CollectionUtils.isEmpty(runnerVos)) {
            jobVo.setStatus(JobStatus.PAUSED.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
        } else {
            runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
            autoexecJobService.checkRunnerHealth(runnerVos);
            JSONObject paramJson = new JSONObject();
            paramJson.put("jobId", jobVo.getId());
            paramJson.put("tenant", TenantContext.get().getTenantUuid());
            paramJson.put("execUser", UserContext.get().getUserUuid(true));
            for (RunnerMapVo runner : runnerVos) {
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getRunnerMapId());
                    //put("phaseSort", jobVo.getCurrentGroupSort());
                }});
                String url = runner.getUrl() + "api/rest/job/pause";
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
