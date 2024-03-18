/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeLogTailHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeLogTailHandler.class);

    @Resource
    AutoexecJobService autoexecJobService;

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
        paramJson.put("resourceId", nodeVo.getResourceId());
        paramJson.put("phase", nodeVo.getJobPhaseName());
        paramJson.put("phaseId", nodeVo.getJobPhaseId());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        paramJson.put("runnerUrl", nodeVo.getRunnerUrl());
        paramJson.put("execMode", phaseVo.getExecMode());
        paramJson.put("direction", StringUtils.isBlank(paramJson.getString("direction")) ? "down" : paramJson.getString("direction"));
        JSONObject result = new JSONObject();
        String nodeStatus = JobNodeStatus.PENDING.getValue();
        if (StringUtils.isBlank(paramJson.getString("sqlName"))) {//获取node节点的状态（包括operation status）
            AutoexecJobPhaseNodeVo phaseNodeVo = autoexecJobService.getNodeOperationStatus(paramJson, false);
            result.put("interact", phaseNodeVo.getInteract());
            if(!StringUtils.isBlank(phaseNodeVo.getStatus())) {
                nodeStatus = phaseNodeVo.getStatus();
            }
        } else {//获取sql 状态
            String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/status/get";
            JSONObject statusJson = JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));
            if (MapUtils.isNotEmpty(statusJson)) {
                result.put("interact", statusJson.get("interact"));
                nodeStatus = statusJson.getString("status");
            }
        }
        paramJson.put("status", nodeStatus);
        if(!Objects.equals(JobNodeStatus.PENDING.getValue(),nodeStatus)) {
            result.putAll(JSONObject.parseObject(AutoexecUtil.requestRunner(paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/log/tail", paramJson)));
        }
        result.put("isRefresh", 0);
        if (Objects.equals(JobNodeStatus.PENDING.getValue(),nodeStatus) || Arrays.asList(JobNodeStatus.RUNNING.getValue(), JobNodeStatus.ABORTING.getValue()).contains(nodeStatus)) {
            result.put("isRefresh", 1);
        }
        result.put("nodeStatus", nodeStatus);
        return result;
    }
}
