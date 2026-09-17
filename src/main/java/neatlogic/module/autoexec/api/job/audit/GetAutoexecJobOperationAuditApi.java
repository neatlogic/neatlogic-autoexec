package neatlogic.module.autoexec.api.job.audit;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobOperationAuditQueryService;

/** 只读查询作业业务操作记录，复用作业详情的基础查看权限。 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobOperationAuditApi extends PrivateApiComponentBase {
    @Resource private AutoexecJobOperationAuditQueryService auditQueryService;
    @Override
    public String getName() { return "autoexec.operationaudit.get"; }
    @Override
    public String getConfig() { return null; }
    @Override
    public String getToken() { return "autoexec/job/operation/audit/get"; }

    /** 仅返回当前租户且属于指定作业的记录与目标快照。 */
    @Input({
        @Param(name = "jobId", type = ApiParamType.LONG, desc = "autoexec.operationaudit.jobid", isRequired = true),
        @Param(name = "id", type = ApiParamType.LONG, desc = "autoexec.operationaudit.id", isRequired = true),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "autoexec.operationaudit.currentpage", help = "autoexec.operationaudit.paginationhelp"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "autoexec.operationaudit.pagesize", help = "autoexec.operationaudit.paginationhelp")
    })
    @Output({
        @Param(explode = AutoexecJobOperationAuditVo.class),
        @Param(name = "targets", type = ApiParamType.JSONOBJECT, desc = "autoexec.operationaudit.targets")
    })
    @Description(desc = "autoexec.operationaudit.get")
    @Override
    public Object myDoService(JSONObject request) {
        return auditQueryService.get(request);
    }
}
