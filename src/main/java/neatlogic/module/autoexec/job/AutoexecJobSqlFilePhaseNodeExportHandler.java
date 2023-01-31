package neatlogic.module.autoexec.job;

import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.dto.INodeDetail;
import neatlogic.framework.autoexec.dto.ISqlNodeDetail;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.AutoexecJobPhaseNodeExportHandlerBase;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoexecJobSqlFilePhaseNodeExportHandler extends AutoexecJobPhaseNodeExportHandlerBase {

    @Override
    public String getName() {
        return ExecMode.SQL.getValue();
    }

    @Override
    protected int getJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo, String source) {
        IAutoexecJobSourceTypeHandler action = AutoexecJobSourceTypeHandlerFactory.getAction(source);
        if (action != null) {
            return action.searchJobPhaseSqlCount(jobPhaseNodeVo);
        }
        return 0;
    }

    @Override
    protected List<? extends INodeDetail> searchJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo, String source) {
        IAutoexecJobSourceTypeHandler action = AutoexecJobSourceTypeHandlerFactory.getAction(source);
        if (action != null) {
            return action.searchJobPhaseSqlForExport(jobPhaseNodeVo);
        }
        return null;
    }

    @Override
    protected void assembleData(AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo, List<? extends INodeDetail> nodeList, Map<Long, Map<String, Object>> nodeDataMap, Map<String, List<Long>> runnerNodeMap, Map<Long, JSONObject> nodeLogTailParamMap, Map<Long, String> nodeOutputParamMap) {
        for (INodeDetail vo : nodeList) {
            ISqlNodeDetail detail = (ISqlNodeDetail) vo;
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("name", detail.getSqlFile());
            dataMap.put("host", detail.getHost() + (detail.getPort() != null ? ":" + detail.getPort() : ""));
            dataMap.put("nodeName", detail.getNodeName());
            dataMap.put("statusName", detail.getStatusName());
            dataMap.put("costTime", detail.getCostTime());
            dataMap.put("startTime", detail.getStartTime() != null ? TimeUtil.convertDateToString(detail.getStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
            dataMap.put("endTime", detail.getEndTime() != null ? TimeUtil.convertDateToString(detail.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
            String runnerHost = vo.getRunnerHost();
            Integer runnerPort = vo.getRunnerPort();
            String runner = StringUtils.EMPTY;
            if (StringUtils.isNotBlank(runnerHost) && runnerPort != null) {
                runner = runnerHost + ":" + runnerPort;
            }
            dataMap.put("runner", runner);
            if (MapUtils.isNotEmpty(nodeOutputParamMap)) {
                dataMap.put("outputParam", nodeOutputParamMap.get(vo.getResourceId()));
            }
            nodeDataMap.put(detail.getId(), dataMap);
            runnerNodeMap.computeIfAbsent(detail.getRunnerUrl(), k -> new ArrayList<>()).add(detail.getId());
            nodeLogTailParamMap.put(detail.getId(), new JSONObject() {
                {
                    this.put("id", detail.getId());
                    this.put("jobId", jobVo.getId());
                    this.put("resourceId", detail.getResourceId());
                    this.put("sqlName", detail.getSqlFile());
                    this.put("phase", phaseVo.getName());
                    this.put("ip", detail.getHost());
                    this.put("port", detail.getPort());
                    this.put("execMode", phaseVo.getExecMode());
                }
            });
        }
    }
}
