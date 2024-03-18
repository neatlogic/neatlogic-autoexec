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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobPhaseNodeStatusApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(UpdateAutoexecJobPhaseNodeStatusApi.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "回调更新作业剧本节点状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "host", type = ApiParamType.STRING, desc = "节点ip"),
            @Param(name = "port", type = ApiParamType.STRING, desc = "节点port"),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数")
    })
    @Output({
    })
    @Description(desc = "回调更新作业剧本节点状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        //避免并发过大，导致性能问题，以下逻辑没必要控制串行
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        Long resourceId = jsonObj.getLong("resourceId");
        String phaseName = jsonObj.getString("phase");
        AutoexecJobPhaseNodeVo nodeVo = null;
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jsonObj.getLong("jobId"), phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(phaseName);
        }
        //获取node
        if (Arrays.asList(ExecMode.RUNNER.getValue(), ExecMode.SQL.getValue()).contains(jobPhaseVo.getExecMode())) {
            List<AutoexecJobPhaseNodeVo> nodeList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobId, jobPhaseVo.getId());
            if (CollectionUtils.isNotEmpty(nodeList)) {
                nodeVo = nodeList.get(0);
            }
        } else {
            nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobIdAndJobPhaseNameAndResourceId(jobId, phaseName, resourceId);
        }
        //不抛异常影响其它节点运行，ignore 就好
        if (nodeVo == null) {
            return null;
        }

        //如果node status 和原本的status 或者  warnCount 一样则无需更新
        if (!Objects.equals(nodeVo.getStatus(), jsonObj.getString("status")) || !Objects.equals(nodeVo.getWarnCount(), jsonObj.getInteger("warnCount"))) {
            nodeVo.setStatus(jsonObj.getString("status"));
            nodeVo.setWarnCount(jsonObj.getInteger("warnCount"));
            if (Objects.equals(nodeVo.getStatus(), JobNodeStatus.RUNNING.getValue())) {
                nodeVo.setIsExecuted(1);
            }
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/status/update";
    }
}
