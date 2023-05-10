/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.autoexec.job.action.handler.node;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.dto.RestVo;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.RestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeLogDownloadHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeLogDownloadHandler.class);

    @Override
    public String getName() {
        return JobAction.DOWNLOAD_NODE_LOG.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("sqlName", paramObj.getString("sqlName"));
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());
        String fileName = FileUtil.getEncodedFileName(nodeVo.getJobPhaseName() + "-" + nodeVo.getHost() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".log");
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        String url = String.format("%s/api/binary/job/phase/node/log/download", nodeVo.getRunnerUrl());
        RestVo restVo = new RestVo.Builder(url, AuthenticateType.BUILDIN.getValue()).setPayload(paramObj).build();
        String result = RestUtil.sendPostRequestForStream(restVo);
        if (StringUtils.isNotBlank(result)) {
            throw new RunnerHttpRequestException(restVo.getUrl() + ":" + result);
        }
        return null;
    }
}
