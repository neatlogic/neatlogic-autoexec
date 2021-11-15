/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectAuthException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectRefusedException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dto.RestVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.RestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
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
        JSONObject jsonObj = jobVo.getActionParam();
        Integer isAll = jsonObj.getInteger("isAll");
        if (!Objects.equals(isAll, 1)) {
            if (CollectionUtils.isEmpty(jsonObj.getJSONArray("resourceIdList"))) {
                throw new ParamIrregularException("resourceIdList");
            }
            List<Long> resourceIdList = JSONObject.parseArray(jsonObj.getJSONArray("resourceIdList").toJSONString(), Long.class);
            List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdList(jobVo.getCurrentPhaseId(), resourceIdList);
            if (CollectionUtils.isEmpty(nodeVoList)) {
                throw new AutoexecJobPhaseNodeNotFoundException(StringUtils.EMPTY, resourceIdList.stream().map(Object::toString).collect(Collectors.joining(",")));
            }
            jobVo.setPhaseNodeVoList(nodeVoList);
        } else {
            jobVo.setPhaseNodeVoList(autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhaseId()));
        }
        return true;
    }


    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        //更新作业状态
        //autoexecJobMapper.updateJobStatus(new AutoexecJobVo(jobVo.getId(),JobStatus.RUNNING.getValue()));
        //更新阶段状态
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getPhaseList().get(0);
        /*List<String> exceptStatus = Collections.singletonList(JobNodeStatus.IGNORED.getValue());
        List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatus(currentPhaseVo.getJobId(), currentPhaseVo.getId(), exceptStatus);
        if(CollectionUtils.isNotEmpty(jobPhaseNodeVoList)&&jobPhaseNodeVoList.size() == 1){//如果该阶段只有一个节点
            currentPhaseVo.setStatus(JobPhaseStatus.PENDING.getValue());
        }else{
            currentPhaseVo.setStatus(JobPhaseStatus.RUNNING.getValue());
        }
        autoexecJobMapper.updateJobPhaseStatus(currentPhaseVo);*/
        //重置节点 (status、starttime、endtime)
        for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getPhaseNodeVoList()) {
            nodeVo.setStatus(JobNodeStatus.PENDING.getValue());
            nodeVo.setStartTime(null);
            nodeVo.setEndTime(null);
            autoexecJobMapper.updateJobPhaseNodeById(nodeVo);
        }
        autoexecJobMapper.updateJobPhaseNodeListStatus(jobVo.getPhaseNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getPhaseNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
        }
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        //清除runner node状态
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
            paramJson.put("phaseNodeList", jobVo.getPhaseNodeVoList());
            for (RunnerMapVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/phase/node/status/reset";
                restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), paramJson);
                result = RestUtil.sendRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + resultJson.getString("Message"));
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
