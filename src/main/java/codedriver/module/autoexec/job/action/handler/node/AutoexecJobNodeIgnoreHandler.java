/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
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
public class AutoexecJobNodeIgnoreHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeResetHandler.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return JobAction.IGNORE_NODE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        List<AutoexecJobPhaseNodeVo> nodeVoList;
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getCurrentPhase();
        //重置mongodb node 状态
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        IAutoexecJobSourceTypeHandler handler = null;
        List<Long> sqlIdList = new ArrayList<>();
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            if (StringUtils.equals(jobVo.getSource(), JobSourceType.DEPLOY.getValue())) {
                handler = AutoexecJobSourceTypeHandlerFactory.getAction(JobSourceType.DEPLOY.getValue());
            } else {
                handler = AutoexecJobSourceTypeHandlerFactory.getAction(codedriver.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue());
            }
            sqlIdList = handler.getSqlIdsAndExecuteJobNodes(jobVo.getActionParam(), jobVo);
            nodeVoList = jobVo.getExecuteJobNodeVoList();
        } else {
            currentResourceIdListValid(jobVo);
            nodeVoList = autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        }
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
        }
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        paramJson.put("phaseName", currentPhaseVo.getName());
        paramJson.put("execMode", currentPhaseVo.getExecMode());
        paramJson.put("phaseNodeList", jobVo.getExecuteJobNodeVoList());
        for (RunnerMapVo runner : runnerVos) {
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
        //更新mysql
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            if (handler != null && CollectionUtils.isNotEmpty(sqlIdList)) {
                handler.updateSqlStatus(sqlIdList, JobNodeStatus.IGNORED.getValue());
            }
        } else {
            for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getExecuteJobNodeVoList()) {
                nodeVo.setStatus(JobNodeStatus.IGNORED.getValue());
                nodeVo.setStartTime(null);
                nodeVo.setEndTime(null);
                autoexecJobMapper.updateJobPhaseNodeById(nodeVo);
            }
        }

        return null;
    }
}
