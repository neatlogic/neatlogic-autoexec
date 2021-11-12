/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仅允许phase 和 node 状态都不是running的情况下才能执行重跑动作
 *
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobPhaseNodeIgnoreApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "忽略作业节点";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.STRING, desc = "作业阶段Id", isRequired = true),
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, desc = "作业节点资产idList", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "忽略作业节点")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setId(jsonObj.getLong("jobId"));
        jobVo.setCurrentPhaseId(jsonObj.getLong("jobPhaseId"));
        jobVo.setActionParam(jsonObj);
        IAutoexecJobActionHandler ignoreNodeAction = AutoexecJobActionHandlerFactory.getAction(JobAction.IGNORE_NODE.getValue());
        return ignoreNodeAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/ignore";
    }
}
