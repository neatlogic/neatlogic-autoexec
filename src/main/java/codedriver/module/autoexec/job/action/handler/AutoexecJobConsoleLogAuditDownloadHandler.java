/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;
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
    public boolean isNeedExecuteAuthCheck() {
        return true;
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
        String fileName = FileUtil.getEncodedFileName(UserContext.get().getRequest().getHeader("User-Agent"), paramObj.getString("jobId") + "-"
                + paramObj.getString("runnerIp") + "-" + paramObj.getString("runnerPort") + TimeUtil.convertDateToString(new Date(paramObj.getLong("startTime")), TimeUtil.YYYYMMDD_HHMMSS)+ ".log");
        String url = String.format("%s/api/binary/job/console/log/audit/download", paramObj.getString("runnerUrl"));
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        String result = HttpRequestUtil.download(url,UserContext.get().getResponse().getOutputStream())
                .setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                .sendRequest().getError();
        if (StringUtils.isNotBlank(result)) {
            throw new AutoexecJobRunnerHttpRequestException(url + ":" + result);
        }
        return null;
    }
}
