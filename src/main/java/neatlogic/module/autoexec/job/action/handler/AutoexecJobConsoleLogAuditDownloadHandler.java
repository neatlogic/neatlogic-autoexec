/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobConsoleLogAuditDownloadHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobConsoleLogAuditDownloadHandler.class);
    @Resource
    RunnerMapper runnerMapper;

    @Override
    public String getName() {
        return JobAction.DOWNLOAD_CONSOLE_LOG_AUDIT.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        Long runnerId = jobVo.getActionParam().getLong("runnerId");
        RunnerVo runnerVo = runnerMapper.getRunnerById(runnerId);
        if (runnerVo == null) {
            throw new AutoexecJobRunnerNotFoundException(runnerId.toString());
        }
        jobVo.getActionParam().put("runnerUrl", runnerVo.getUrl());
        jobVo.getActionParam().put("runnerIp", runnerVo.getHost());
        jobVo.getActionParam().put("runnerPort", runnerVo.getPort());
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        JSONObject paramObj = jobVo.getActionParam();
        String fileName = FileUtil.getEncodedFileName(paramObj.getString("jobId") + "-"
                + paramObj.getString("runnerIp") + "-" + paramObj.getString("runnerPort") + TimeUtil.convertDateToString(new Date(paramObj.getLong("startTime")), TimeUtil.YYYYMMDD_HHMMSS) + ".log");
        String url = String.format("%s/api/binary/job/console/log/audit/download", paramObj.getString("runnerUrl"));
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream())
                .setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                .sendRequest().getError();
        if (StringUtils.isNotBlank(result)) {
            throw new RunnerHttpRequestException(url + ":" + result);
        }
        return null;
    }
}
