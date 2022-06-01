/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeLogTailHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeLogTailHandler.class);

    @Override
    public String getName() {
        return JobAction.TAIL_NODE_LOG.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        JSONObject paramJson = jobVo.getActionParam();
        paramJson.put("jobId", phaseVo.getJobId());
        paramJson.put("nodeId", nodeVo.getId());
        paramJson.put("resourceId", nodeVo.getResourceId());
        paramJson.put("phase", nodeVo.getJobPhaseName());
        paramJson.put("phaseId", nodeVo.getJobPhaseId());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        paramJson.put("runnerUrl", nodeVo.getRunnerUrl());
        paramJson.put("execMode", phaseVo.getExecMode());
        paramJson.put("direction", "down");
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/log/tail";
        JSONObject result = JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));
        result.put("isRefresh", 0);
        if(StringUtils.isBlank(paramJson.getString("sqlName"))) {//获取node节点的状态（包括operation status）
            AutoexecJobPhaseNodeVo phaseNodeVo = getNodeOperationStatus(paramJson);
            List<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVos = phaseNodeVo.getOperationStatusVoList();
          /*  for (AutoexecJobPhaseNodeOperationStatusVo statusVo : operationStatusVos) {
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
                if (Objects.equals(paramJson.getString("status"),JobNodeStatus.SUCCEED.getValue())&&Objects.equals(statusVo.getStatus(), JobNodeStatus.FAILED.getValue()) && Objects.equals(statusVo.getFailIgnore(), 0)) {
                    result.put("isRefresh", 0);
                    break;
                }
            }*/
            result.put("operationStatusList", operationStatusVos);
            result.put("interact",phaseNodeVo.getInteract());
            String nodeStatusOld = paramJson.getString("status");
            if(Objects.equals(nodeStatusOld,JobNodeStatus.RUNNING.getValue()) || Objects.equals(phaseNodeVo.getStatus(),JobNodeStatus.RUNNING.getValue())){
                result.put("isRefresh", 1);
            }
        }else{//获取sql 状态
            AutoexecJobNodeSqlVo sqlStatusVo = getNodeSqlStatus(paramJson);
            if(sqlStatusVo != null && !Objects.equals(sqlStatusVo.getStatus(), JobNodeStatus.SUCCEED.getValue())){
                result.put("isRefresh", 1);
            }
        }
        return result;
    }
}
