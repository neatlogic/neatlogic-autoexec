/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/21 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobNodeSubmitWaitInputApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getName() {
        return "提交作业节点waitInput";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.STRING, desc = "作业阶段Id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "option", type = ApiParamType.STRING, desc = "提交选项", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "提交作业节点waitInput接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        Long jobPhaseId = paramObj.getLong("jobPhaseId");
        Long resourceId = paramObj.getLong("resourceId");
        String option = paramObj.getString("option");
        String sqlName = paramObj.getString("sqlName");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(jobId, jobPhaseId);
        if (phaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobPhaseId.toString());
        }
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(jobPhaseId,resourceId);
        if (nodeVo == null) {
            throw new AutoexecJobPhaseNodeNotFoundException(jobPhaseId.toString(), resourceId == null? StringUtils.EMPTY:resourceId.toString());
        }
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("sqlName",sqlName);
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("phaseId", nodeVo.getJobPhaseId());
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());
        paramObj.put("option", option);
        //获取pipeFile 路径
        AutoexecJobPhaseNodeVo phaseNodeVo = autoexecJobActionService.getNodeOperationStatus(paramObj);
        JSONObject interactJson = phaseNodeVo.getInteract();
        if(MapUtils.isNotEmpty(interactJson)){
            paramObj.put("pipeFile",interactJson.getString("pipeFile"));
        }
        autoexecJobActionService.submitWaitInput(paramObj);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/node/submit/waitInput";
    }
}
