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
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectRefusedException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dto.RestVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.RestUtil;
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
        if (Objects.equals(jobVo.getAction(), JobAction.RESET_REFIRE.getValue())) {
            if (CollectionUtils.isEmpty(jobVo.getPhaseList())) {
                jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()));
            }
            new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
            jobVo.setStatus(JobStatus.PENDING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
            resetAll(jobVo);
            autoexecJobMapper.updateJobPhaseStatusByJobId(jobVo.getId(), JobPhaseStatus.PENDING.getValue());//重置phase状态为pending
            autoexecJobMapper.updateJobPhaseFailedNodeStatusByJobId(jobVo.getId(), JobNodeStatus.PENDING.getValue());
            autoexecJobService.getAutoexecJobDetail(jobVo, 0);
            jobVo.setCurrentPhaseSort(0);
            autoexecJobService.refreshJobPhaseNodeList(jobVo.getId(), jobVo.getCurrentPhaseSort(), null);
        } else if (Objects.equals(jobVo.getAction(), JobAction.REFIRE.getValue())) {
            int sort = 0;
            /*寻找中止|暂停|失败的phase
             * 1、优先寻找pending|aborted|paused|failed phaseList
             * 2、没有满足1条件的，再寻找pending|aborted|paused|failed node 最小sort phaseList
             */
            List<AutoexecJobPhaseVo> autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            if (CollectionUtils.isEmpty(autoexecJobPhaseVos)) {
                autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndNodeStatusList(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(), JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            }
            //如果都成功了则无须重跑
            if(CollectionUtils.isEmpty(autoexecJobPhaseVos)){
                return null;
            }
            sort = autoexecJobPhaseVos.get(0).getSort();
            //int finalSort = sort;
            //List<Long> jobPhaseIdList = autoexecJobPhaseVos.stream().filter(p->p.getSort() == finalSort).map(AutoexecJobPhaseVo::getId).collect(Collectors.toList());
            jobVo.setCurrentPhaseSort(sort);
            autoexecJobService.getAutoexecJobDetail(jobVo, sort);
            //补充配置，只保留满足条件（该sort下，未开始、失败、已暂停或已中止）的phase
            //jobVo.setPhaseList(jobVo.getPhaseList().stream().filter(o -> jobPhaseIdList.contains(o.getId())).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(jobVo.getPhaseList())) {
                new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
            }
        }
        if (CollectionUtils.isNotEmpty(jobVo.getPhaseList())) {
            execute(jobVo);
        }
        return null;
    }

    /**
     * 重置作业
     *
     * @param jobVo 作业
     */
    private void resetAll(AutoexecJobVo jobVo) {
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobVo.getId(), jobVo.getPhaseIdList());
        if (CollectionUtils.isEmpty(runnerVos)) {
            throw new AutoexecJobRunnerNotFoundException(jobVo.getPhaseNameList());
        }
        checkRunnerHealth(runnerVos);
        try {
            for (RunnerMapVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/all/reset";
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getRunnerMapId());
                    put("phaseSort", jobVo.getCurrentPhaseSort());
                }});
                restVo = new RestVo.Builder(url, AuthenticateType.BUILDIN.getValue()).setPayload(paramJson).build();
                result = RestUtil.sendPostRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerHttpRequestException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
    }
}
