/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.job.action.node;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
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
public class DownloadAutoexecJobPhaseNodeAuditApi extends PrivateBinaryStreamApiComponentBase {
    @Override
    public String getName() {
        return "下载作业剧本节点操作记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "startTime", type = ApiParamType.STRING, desc = "执行开始时间", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "执行状态", isRequired = true),
            @Param(name = "execUser", type = ApiParamType.STRING, desc = "执行用户", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "获取作业剧本节点操作记录")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.DOWNLOAD_NODE_AUDIT.getValue());
        IAutoexecJobActionHandler downloadNodeAuditAction = AutoexecJobActionHandlerFactory.getAction(JobAction.DOWNLOAD_NODE_AUDIT.getValue());
        return downloadNodeAuditAction.doService(jobVo).get("auditContent");
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/audit/download";
    }


}
