/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeOperationStatusVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/5/13 16:49
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeLogTailApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取剧本节点执行日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取位置,-1:获取最新的数据"),
            @Param(name = "direction", type = ApiParamType.ENUM, rule = "up,down", isRequired = true, desc = "读取方向，up:向上读，down:向下读")
    })
    @Output({
            @Param(name = "tailContent", type = ApiParamType.LONG, isRequired = true, desc = "内容"),
            @Param(name = "startPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取开始位置"),
            @Param(name = "endPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取结束位置"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "读取到的位置"),
            @Param(name = "last", type = ApiParamType.LONG, isRequired = true, desc = "日志读取内容"),
            @Param(name = "operationStatusList", explode = AutoexecJobPhaseNodeOperationStatusVo[].class, desc = "作业剧本节点操作状态列表"),
            @Param(name = "isRefresh", type = ApiParamType.INTEGER, isRequired = true, desc = "是否需要继续定时刷新，1:继续 0:停止")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result;
        Long phaseId = paramObj.getLong("jobPhaseId");
        Long resourceId = paramObj.getLong("resourceId");
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(phaseId,resourceId);
        if (nodeVo == null) {
            throw new AutoexecJobPhaseNodeNotFoundException(phaseId.toString(), resourceId == null?StringUtils.EMPTY:resourceId.toString());
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(nodeVo.getJobId(), nodeVo.getJobPhaseId());
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("phaseId", nodeVo.getJobPhaseId());
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());
        paramObj.put("direction", "down");
        result = autoexecJobActionService.tailNodeLog(paramObj);
        result.put("isRefresh", 0);
        List<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVos = autoexecJobActionService.getNodeOperationStatus(paramObj);
        for (AutoexecJobPhaseNodeOperationStatusVo statusVo : operationStatusVos) {
            //如果存在pending|running 的节点|阶段 则继续tail
            //如果operation的状态为null，表示还没刷新结果，继续tail
            if (Objects.equals(phaseVo.getStatus(), JobPhaseStatus.PENDING.getValue())
                    || Objects.equals(phaseVo.getStatus(), JobPhaseStatus.RUNNING.getValue())
                    || StringUtils.isBlank(statusVo.getStatus())
                    || Objects.equals(statusVo.getStatus(), JobNodeStatus.PENDING.getValue())
                    || Objects.equals(statusVo.getStatus(), JobNodeStatus.RUNNING.getValue())) {
                result.put("isRefresh", 1);
                break;
            }
            //如果存在失败停止策略的操作节点，则停止tail
            if (Objects.equals(statusVo.getStatus(), JobNodeStatus.FAILED.getValue()) && Objects.equals(statusVo.getFailIgnore(), 0)) {
                result.put("isRefresh", 0);
                break;
            }
        }
        result.put("operationStatusList", operationStatusVos);
        return result;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/phase/node/log/tail";
    }
}
