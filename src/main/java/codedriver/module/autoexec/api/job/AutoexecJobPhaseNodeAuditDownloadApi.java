/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobHostPortRunnerNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeAuditDownloadApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

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
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id", isRequired = true),
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
        JSONObject result = new JSONObject();
        Long phaseId = paramObj.getLong("jobPhaseId");
        Long resourceId = paramObj.getLong("resourceId");
        String startTime = paramObj.getString("startTime");
        String execUser = paramObj.getString("execUser");
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(phaseId, resourceId);
        if (nodeVo == null) {
            throw new AutoexecJobPhaseNodeNotFoundException(phaseId.toString(), resourceId == null ? StringUtils.EMPTY : resourceId.toString());
        }
        if (StringUtils.isBlank(nodeVo.getRunnerUrl())) {
            throw new AutoexecJobHostPortRunnerNotFoundException(nodeVo.getHost() + ":" + nodeVo.getPort());
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(nodeVo.getJobId(), nodeVo.getJobPhaseId());
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("sqlName", paramObj.getString("sqlName"));
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());
        String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"),
                nodeVo.getJobPhaseName() + "-" + nodeVo.getHost() + "-" + nodeVo.getResourceId() + "-" + TimeUtil.convertStringToDate(startTime, TimeUtil.YYYYMMDD_HHMMSS) + "-" + execUser + ".txt");
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        autoexecJobActionService.downloadNodeAudit(paramObj, response);
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/audit/download";
    }


}
