/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeOperationStatusVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeOutputParamGetApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取作业节点输出参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id")
    })
    @Output({
            @Param(explode = AutoexecJobPhaseNodeOperationStatusVo[].class, desc = "作业剧本节点操作状态列表"),
    })
    @Description(desc = "获取作业节点输出参数")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setId(paramObj.getLong("jobId"));
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.GET_NODE_OUTPUT_PARAM.getValue());
        IAutoexecJobActionHandler getNodeOutputParamAction = AutoexecJobActionHandlerFactory.getAction(JobAction.GET_NODE_OUTPUT_PARAM.getValue());
        return getNodeOutputParamAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/output/param/get";
    }


}
