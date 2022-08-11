/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadAutoexecJobConsoleLogApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "/autoexec/job/console/log/download";
    }

    @Override
    public String getName() {
        return "下载作业控制台执行日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业Id"),
            @Param(name = "runnerId", type = ApiParamType.LONG, isRequired = true, desc = "runnerId")
    })
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.DOWNLOAD_CONSOLE_LOG.getValue());
        IAutoexecJobActionHandler nodeAuditListAction = AutoexecJobActionHandlerFactory.getAction(JobAction.DOWNLOAD_CONSOLE_LOG.getValue());
        return nodeAuditListAction.doService(jobVo);
    }

}
