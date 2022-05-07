/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeAuditVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobConsoleLogAuditListApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取作业控制台操作记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业Id"),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "runner Id")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobPhaseNodeAuditVo[].class, desc = "节点操作记录列表")
    })
    @Description(desc = "获取作业控制台操作记录")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.CONSOLE_LOG_AUDIT_LIST.getValue());
        IAutoexecJobActionHandler nodeAuditListAction = AutoexecJobActionHandlerFactory.getAction(JobAction.CONSOLE_LOG_AUDIT_LIST.getValue());
        return nodeAuditListAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/console/log/audit/list";
    }


}
