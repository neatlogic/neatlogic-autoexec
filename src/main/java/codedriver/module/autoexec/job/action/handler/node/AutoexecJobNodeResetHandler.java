/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectRefusedException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerFactory;
import codedriver.framework.autoexec.job.source.action.IAutoexecJobSourceActionHandler;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.dto.RestVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.RestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeResetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeResetHandler.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return JobAction.RESET_NODE.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        return true;
    }


    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        List<AutoexecJobPhaseNodeVo> nodeVoList;
        //更新状态
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getCurrentPhase();
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            jobVo.getActionParam().put("phaseName", currentPhaseVo.getName());
            IAutoexecJobSourceActionHandler handler;
            if (StringUtils.equals(jobVo.getSource(), DeployOperType.DEPLOY.getValue())) {
                handler = AutoexecJobSourceActionHandlerFactory.getAction(DeployOperType.DEPLOY.getValue());
            } else {
                handler = AutoexecJobSourceActionHandlerFactory.getAction(AutoexecOperType.AUTOEXEC.getValue());
            }
            handler.resetSqlStatus(jobVo.getActionParam(), jobVo);
            nodeVoList = jobVo.getExecuteJobNodeVoList();
        } else {
            Integer isAll = jobVo.getActionParam().getInteger("isAll");
            if (!Objects.equals(isAll, 1)) {
                currentResourceIdListValid(jobVo);
            } else {
                jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhaseId()));
            }
            //重置节点 (status、startTime、endTime)
            for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getExecuteJobNodeVoList()) {
                nodeVo.setStatus(JobNodeStatus.PENDING.getValue());
                nodeVo.setStartTime(null);
                nodeVo.setEndTime(null);
                autoexecJobMapper.updateJobPhaseNodeById(nodeVo);
            }
            autoexecJobMapper.updateJobPhaseNodeListStatus(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());
            nodeVoList = autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        }
        //重置mongodb node 状态
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
        }
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        try {
            JSONObject paramJson = new JSONObject();
            paramJson.put("jobId", jobVo.getId());
            paramJson.put("tenant", TenantContext.get().getTenantUuid());
            paramJson.put("execUser", UserContext.get().getUserUuid(true));
            paramJson.put("phaseName", currentPhaseVo.getName());
            paramJson.put("execMode", currentPhaseVo.getExecMode());
            paramJson.put("phaseNodeList", jobVo.getExecuteJobNodeVoList());
            for (RunnerMapVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/phase/node/status/reset";
                restVo = new RestVo.Builder(url, AuthenticateType.BUILDIN.getValue()).setPayload(paramJson).build();
                result = RestUtil.sendPostRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerHttpRequestException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            assert restVo != null;
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
        return null;
    }
}
