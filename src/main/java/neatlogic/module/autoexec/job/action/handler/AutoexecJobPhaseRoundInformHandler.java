/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

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
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.INFORM_PHASE_ROUND.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        JSONObject jsonObj = jobVo.getActionParam();
        Integer groupSort = jsonObj.getInteger("groupNo");
        JSONObject informParam = new JSONObject();
        informParam.put("action", "informRoundContinue");
        informParam.put("groupNo", groupSort);
        jsonObj.put("informParam", informParam);
        jsonObj.put("socketFileName", "job" + jsonObj.getString("execId"));
        AutoexecJobPhaseVo phaseVo = jobVo.getExecutePhase();
        //寻找下一个phase执行当前round,如果不存在下一个phase 则啥都不做
        //AutoexecJobPhaseVo nextJobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndGroupSortAndSort(phaseVo.getJobId(), groupSort, phaseVo.getSort() + 1);
        //if (nextJobPhaseVo != null) {
        informParam.put("phaseName", phaseVo.getName());
        informParam.put("roundNo", jsonObj.getInteger("roundNo"));
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobRunnerListByJobIdAndGroupId(phaseVo.getJobId(), phaseVo.getGroupId());
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        autoexecJobService.checkRunnerHealth(runnerVos);
        for (RunnerMapVo runnerVo : runnerVos) {
            String url = String.format("%s/api/rest/job/phase/socket/write", runnerVo.getUrl());
            String result = HttpRequestUtil.post(url)
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(Config.RUNNER_CONNECT_TIMEOUT()).setReadTimeout(Config.RUNNER_READ_TIMEOUT())
                    .sendRequest().getError();
            if (StringUtils.isNotBlank(result)) {
                throw new RunnerHttpRequestException(url + ":" + result);
            }
        }
        //}
        return null;
    }

}
