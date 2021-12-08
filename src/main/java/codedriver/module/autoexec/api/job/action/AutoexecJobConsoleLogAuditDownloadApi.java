/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobConsoleLogAuditDownloadApi extends PrivateBinaryStreamApiComponentBase {
    @Override
    public String getName() {
        return "下载作业控制台操作记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业Id"),
            @Param(name = "startTime", type = ApiParamType.STRING, desc = "执行开始时间", isRequired = true),
            @Param(name = "execUser", type = ApiParamType.STRING, desc = "执行用户", isRequired = true)
    })
    @Output({})
    @Description(desc = "下载作业控制台操作记录")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setActionParam(paramObj);
        IAutoexecJobActionHandler nodeAuditListAction = AutoexecJobActionHandlerFactory.getAction(JobAction.DOWNLOAD_CONSOLE_LOG_AUDIT.getValue());
        return nodeAuditListAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/console/log/audit/download";
    }


}
