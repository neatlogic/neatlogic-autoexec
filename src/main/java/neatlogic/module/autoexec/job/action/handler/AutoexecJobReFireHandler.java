/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobActionInvalidException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseRunnerNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.crossover.IDeployBatchJobCrossoverService;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.autoexec.core.AutoexecJobAuthActionManager;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobReFireHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.REFIRE.getValue();
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
        //List<String> needSqlFileResetStatusPhaseNameList = new ArrayList<>();
        if (Objects.equals(jobVo.getAction(), JobAction.RESET_REFIRE.getValue())) {
            new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
            jobVo.setStatus(JobStatus.PENDING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
            resetAll(jobVo);
            autoexecJobMapper.updateJobPhaseStatusByJobId(jobVo.getId(), JobPhaseStatus.PENDING.getValue());//重置phase状态为pending
            //autoexecJobService.getAutoexecJobDetail(jobVo, 0);
            //获取group
            jobVo.setExecuteJobGroupVo(autoexecJobMapper.getJobGroupByJobIdAndSort(jobVo.getId(), 0));
            //重刷所有phase node
            autoexecJobService.refreshJobNodeList(jobVo.getId());
            //更新没有删除的节点为"未开始"状态
            autoexecJobMapper.updateJobPhaseNodeStatusByJobIdAndIsDelete(jobVo.getId(), JobNodeStatus.PENDING.getValue(), 0);
            jobVo.setIsFirstFire(1);
            //needSqlFileResetStatusPhaseNameList = autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()).stream().filter(o -> Objects.equals(o.getExecMode(), ExecMode.SQL.getValue())).map(AutoexecJobPhaseVo::getName).collect(Collectors.toList());
        } else if (Objects.equals(jobVo.getAction(), JobAction.REFIRE.getValue())) {
            /*寻找中止|暂停|失败的phase
             * 1、寻找pending|aborted|paused|failed phaseList
             * 2、没有满足1条件的,再寻找pending|aborted|paused|failed node 最小sort phaseList
             */
            List<AutoexecJobPhaseVo> autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            if (CollectionUtils.isEmpty(autoexecJobPhaseVos)) {
                autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndNodeStatusList(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            }
            //如果都成功了则无须重跑
            if (CollectionUtils.isEmpty(autoexecJobPhaseVos)) {
                IDeployBatchJobCrossoverService iDeployBatchJobCrossoverService = CrossoverServiceFactory.getApi(IDeployBatchJobCrossoverService.class);
                iDeployBatchJobCrossoverService.checkAndFireLaneNextGroupByJobId(jobVo.getId(), jobVo.getPassThroughEnv());
                jobVo.setStatus(JobStatus.COMPLETED.getValue());
                autoexecJobMapper.updateJobStatus(jobVo);
                return null;
            }
            jobVo.setStatus(JobStatus.PENDING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
            //needSqlFileResetStatusPhaseNameList = autoexecJobPhaseVos.stream().filter(o -> Objects.equals(o.getExecMode(), ExecMode.SQL.getValue())).map(AutoexecJobPhaseVo::getName).collect(Collectors.toList());
            autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(autoexecJobPhaseVos.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()), JobPhaseStatus.PENDING.getValue());
            jobVo.setExecuteJobGroupVo(autoexecJobPhaseVos.get(0).getJobGroupVo());
            autoexecJobService.getAutoexecJobDetail(jobVo);
            if (CollectionUtils.isNotEmpty(jobVo.getPhaseList())) {
                new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
            }
            jobVo.setIsNoFireNext(0);
        }else{
            throw new AutoexecJobActionInvalidException();
        }
        /*if (CollectionUtils.isNotEmpty(needSqlFileResetStatusPhaseNameList)) {
            autoexecJobService.resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameList(jobVo.getId(), needSqlFileResetStatusPhaseNameList);
        }*/
        autoexecJobService.executeGroup(jobVo);
        return null;
    }

    /**
     * 重置runner autoexec 作业
     *
     * @param jobVo 作业
     */
    private void resetAll(AutoexecJobVo jobVo) {
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), jobVo.getPhaseIdList());
        if (CollectionUtils.isEmpty(runnerVos)) {
            throw new AutoexecJobPhaseRunnerNotFoundException(jobVo.getPhaseNameList().stream().map(Object::toString).collect(Collectors.joining("','")));
        }
        autoexecJobService.checkRunnerHealth(runnerVos);

        for (RunnerMapVo runner : runnerVos) {
            String url = runner.getUrl() + "api/rest/job/all/reset";
            paramJson.put("passThroughEnv", new JSONObject() {{
                put("runnerId", runner.getRunnerMapId());
                //put("phaseSort", jobVo.getCurrentGroupSort());
            }});

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
}
