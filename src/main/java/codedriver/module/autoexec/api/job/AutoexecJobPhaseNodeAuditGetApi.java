/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeAuditVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobHostPortRunnerNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeAuditGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "获取作业剧本节点操作记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nodeId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本节点Id")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobPhaseNodeAuditVo[].class, desc = "节点操作记录列表")
    })
    @Description(desc = "获取作业剧本节点操作记录")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobNodeId(paramObj.getLong("nodeId"));
        if(nodeVo == null){
            throw new AutoexecJobPhaseNodeNotFoundException(StringUtils.EMPTY,paramObj.getString("nodeId"));
        }
        if(StringUtils.isBlank(nodeVo.getRunnerUrl())){
            throw new AutoexecJobHostPortRunnerNotFoundException(nodeVo.getHost()+":"+nodeVo.getPort());
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(nodeVo.getJobId(),nodeVo.getJobPhaseId());
        paramObj.put("jobId",nodeVo.getJobId());
        paramObj.put("phase",nodeVo.getJobPhaseName());
        paramObj.put("nodeId", nodeVo.getJobId());
        paramObj.put("ip",nodeVo.getHost());
        paramObj.put("port",nodeVo.getPort());
        paramObj.put("runnerUrl",nodeVo.getRunnerUrl());
        paramObj.put("execMode",phaseVo.getExecMode());
        result.put("tbodyList", autoexecJobActionService.getNodeAudit(paramObj));
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/audit/get";
    }


}
