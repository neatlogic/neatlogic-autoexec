/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
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
public class AutoexecJobPhaseRoundInformHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return JobAction.INFORM_PHASE_ROUND.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return false;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        JSONObject jsonObj = jobVo.getActionParam();
        Integer groupSort = jsonObj.getInteger("groupNo");
        JSONObject informParam = new JSONObject();
        informParam.put("action", "informRoundContinue");
        informParam.put("groupNo", groupSort);
        jsonObj.put("informParam", informParam);
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        //寻找下一个phase执行当前round,如果不存在下一个phase 则啥都不做
        //AutoexecJobPhaseVo nextJobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndGroupSortAndSort(phaseVo.getJobId(), groupSort, phaseVo.getSort() + 1);
        //if (nextJobPhaseVo != null) {
        informParam.put("phaseName", phaseVo.getName());
        informParam.put("roundNo", jsonObj.getInteger("roundNo"));
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobRunnerListByJobIdAndGroupId(phaseVo.getJobId(), phaseVo.getGroupId());
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        for (RunnerMapVo runnerVo : runnerVos) {
            String url = String.format("%s/api/rest/job/phase/round/inform", runnerVo.getUrl());
            String result = HttpRequestUtil.post(url)
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .sendRequest().getError();
            if (StringUtils.isNotBlank(result)) {
                throw new AutoexecJobRunnerHttpRequestException(url + ":" + result);
            }
        }
        //}
        return null;
    }

}