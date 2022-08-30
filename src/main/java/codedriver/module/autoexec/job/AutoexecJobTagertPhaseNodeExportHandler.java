package codedriver.module.autoexec.job;

import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.dto.INodeDetail;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.AutoexecJobPhaseNodeExportHandlerBase;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoexecJobTagertPhaseNodeExportHandler extends AutoexecJobPhaseNodeExportHandlerBase {

    @Override
    public String getName() {
        return ExecMode.TARGET.getValue();
    }

    @Override
    protected int getJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo, String source) {
        IAutoexecJobSourceTypeHandler action = AutoexecJobSourceTypeHandlerFactory.getAction(source);
        if (action != null) {
            return action.searchJobPhaseNodeCount(jobPhaseNodeVo);
        }
        return 0;
    }

    @Override
    protected List<? extends INodeDetail> searchJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo, String source) {
        IAutoexecJobSourceTypeHandler action = AutoexecJobSourceTypeHandlerFactory.getAction(source);
        if (action != null) {
            return action.searchJobPhaseNodeForExport(jobPhaseNodeVo);
        }
        return null;
    }

    @Override
    protected void assembleData(AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo, List<? extends INodeDetail> nodeList, Map<Long, Map<String, Object>> nodeDataMap, Map<String, List<Long>> runnerNodeMap, Map<Long, JSONObject> nodeLogTailParamMap) {
        for (INodeDetail vo : nodeList) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("host", vo.getHost() + (vo.getPort() != null ? ":" + vo.getPort() : ""));
            dataMap.put("nodeName", vo.getNodeName());
            dataMap.put("statusName", vo.getStatusName());
            dataMap.put("costTime", vo.getCostTime());
            dataMap.put("startTime", vo.getStartTime() != null ? TimeUtil.convertDateToString(vo.getStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
            dataMap.put("endTime", vo.getEndTime() != null ? TimeUtil.convertDateToString(vo.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
            nodeDataMap.put(vo.getId(), dataMap);
            runnerNodeMap.computeIfAbsent(vo.getRunnerUrl(), k -> new ArrayList<>()).add(vo.getId());
            nodeLogTailParamMap.put(vo.getId(), new JSONObject() {
                {
                    this.put("id", vo.getId());
                    this.put("jobId", jobVo.getId());
                    this.put("resourceId", vo.getResourceId());
                    this.put("phase", phaseVo.getName());
                    this.put("ip", vo.getHost());
                    this.put("port", vo.getPort());
                    this.put("execMode", phaseVo.getExecMode());
                }
            });
        }
    }
}
