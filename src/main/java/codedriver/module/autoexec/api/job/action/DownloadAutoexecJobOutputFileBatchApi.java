/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicBinaryStreamApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/21 15:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class DownloadAutoexecJobOutputFileBatchApi extends PublicBinaryStreamApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "批量下载作业输出文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "批量下载作业输出文件")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobInfo = autoexecJobMapper.getJobInfo(jobId);
        if (jobInfo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        UserContext.init(SystemUser.SYSTEM.getUserVo(),SystemUser.SYSTEM.getTimezone());
        UserContext.get().setResponse(response);
        List<RunnerVo> runnerVoList = autoexecJobMapper.getJobRunnerListByJobId(jobId);
        for(RunnerVo runnerVo : runnerVoList){
            String url = String.format("%s/api/binary/job/output/file/batch/download", runnerVo.getUrl());
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream()).setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                throw new RunnerHttpRequestException(url + ":" + error);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/output/file/batch/download";
    }
}
