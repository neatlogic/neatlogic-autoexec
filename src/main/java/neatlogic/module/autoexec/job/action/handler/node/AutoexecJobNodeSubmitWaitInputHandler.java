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

package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.job.audit.JobOperationAuditContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobInteractException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 在作业事务内校验并提交节点交互。
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeSubmitWaitInputHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.SUBMIT_NODE_WAIT_INPUT.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    /** 提交人工交互，并向审计上下文提供经过白名单处理的选项。 */
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getExecutePhase();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("phaseId", nodeVo.getJobPhaseId());
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());
        AutoexecJobPhaseNodeVo phaseNodeVo = autoexecJobService.getNodeOperationStatus(paramObj, false);
        JSONObject interactJson = phaseNodeVo.getInteract();
        if (MapUtils.isEmpty(interactJson) || StringUtils.isBlank(interactJson.getString("pipeFile"))) {
            throw new AutoexecJobInteractException("job/phase/node/status/get", paramObj, $.t("autoexec.waitinput.missinginteraction"));
        }
        paramObj.put("pipeFile", interactJson.getString("pipeFile"));
        String option = paramObj.getString("option");
        // 动态按钮只保存服务端定义的选项，自由输入默认脱敏。
        JobOperationAuditContext.interaction(interactJson, option);
        if (StringUtils.isNotBlank(option)) {
            paramObj.put("option", String.format("[%s]# ", UserContext.get().getUserUuid(true)) + option);
        }
        String url = String.format("%s/api/rest/job/phase/node/submit/waitInput", nodeVo.getRunnerUrl());
        AutoexecUtil.requestRunner(url, paramObj);
        return null;
    }
}
