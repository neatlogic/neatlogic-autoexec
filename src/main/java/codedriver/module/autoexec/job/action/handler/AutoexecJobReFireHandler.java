/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobActionInvalidException;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.crossover.IDeployBatchJobCrossoverService;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
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
             * 1、寻找pending|aborted|paused|failed node 最小sort phaseList
             * 2、没有满足1条件的，再寻找pending|aborted|paused|failed phaseList
             */
            List<AutoexecJobPhaseVo> autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndNodeStatusList(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            if (CollectionUtils.isEmpty(autoexecJobPhaseVos)) {
                autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
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
            throw new AutoexecJobRunnerNotFoundException(jobVo.getPhaseNameList());
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
