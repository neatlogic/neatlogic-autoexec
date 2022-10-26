/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseOperationLiveDataApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取阶段工具个性化数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id"),
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "工具id"),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobPhaseId = paramObj.getLong("jobPhaseId");
        Long operationId = paramObj.getLong("operationId");
        AutoexecJobPhaseOperationVo operation = autoexecJobMapper.getMaxSortJobPhaseOperationByPhaseIdAndOperationId(jobPhaseId, operationId);
        if (operation != null) {
            paramObj.put("jobPhaseOperationId", operation.getId());
            paramObj.put("operationName", operation.getName());
            AutoexecJobVo jobVo = new AutoexecJobVo();
            jobVo.setActionParam(paramObj);
            jobVo.setAction(JobAction.GET_OPERATION_LIVE_DATA.getValue());
            IAutoexecJobActionHandler action = AutoexecJobActionHandlerFactory.getAction(JobAction.GET_OPERATION_LIVE_DATA.getValue());
            return action.doService(jobVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/phase/operation/livedata/get";
    }
}
