package neatlogic.module.autoexec.job;

import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.INodeDetail;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.AutoexecJobPhaseNodeExportHandlerBase;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoexecJobRunnerPhaseNodeExportHandler extends AutoexecJobPhaseNodeExportHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return ExecMode.RUNNER.getValue();
    }

    @Override
    protected int getJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo, String source) {
        return autoexecJobMapper.getJobPhaseRunnerNodeByJobIdAndPhaseId(jobPhaseNodeVo.getJobId(), jobPhaseNodeVo.getJobPhaseId()) != null ? 1 : 0;
    }

    /**
     * 查询 Runner 阶段节点，并将其输出资源标识对齐为本地节点 0。
     */
    @Override
    protected List<? extends INodeDetail> searchJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo, String source) {
        AutoexecJobPhaseNodeVo node = autoexecJobMapper.getJobPhaseRunnerNodeByJobIdAndPhaseId(jobPhaseNodeVo.getJobId(), jobPhaseNodeVo.getJobPhaseId());
        if (node != null) {
            // Runner 阶段以本地节点标识 0 持久化输出，导出查询必须使用相同标识。
            node.setResourceId(0L);
        }
        return node != null ? Collections.singletonList(node) : Collections.emptyList();
    }

    /**
     * 组装 Runner 阶段的 Excel 行数据，包含从本地资源输出中读取的参数。
     */
    @Override
    protected void assembleData(AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo, List<? extends INodeDetail> nodeList, Map<Long, Map<String, Object>> nodeDataMap, Map<String, List<Long>> runnerNodeMap, Map<Long, JSONObject> nodeLogTailParamMap, Map<Long, String> nodeOutputParamMap) {
        if (CollectionUtils.isNotEmpty(nodeList)) {
            INodeDetail node = nodeList.get(0);
            RunnerVo runner = autoexecJobMapper.getJobRunnerById(node.getRunnerId());
            if (runner != null) {
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("host", runner.getHost() + ":" + runner.getPort());
                dataMap.put("nodeName", runner.getName());
                dataMap.put("statusName", node.getStatusName());
                dataMap.put("costTime", node.getCostTime());
                dataMap.put("startTime", node.getStartTime() != null ? TimeUtil.convertDateToString(node.getStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
                dataMap.put("endTime", node.getEndTime() != null ? TimeUtil.convertDateToString(node.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
                dataMap.put("runner", runner.getHost() + ":" + runner.getPort());
                if (MapUtils.isNotEmpty(nodeOutputParamMap)) {
                    dataMap.put("outputParam", nodeOutputParamMap.get(node.getResourceId()));
                }
                nodeDataMap.put(node.getId(), dataMap);
            }
        }
    }
}
