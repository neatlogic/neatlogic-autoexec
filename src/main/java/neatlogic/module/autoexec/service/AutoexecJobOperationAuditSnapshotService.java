package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;

/** 将各来源的业务对象投影为允许保存的字段，不持久化业务 VO 或完整请求。 */
@Service
public class AutoexecJobOperationAuditSnapshotService {
    @Resource private AutoexecJobMapper jobMapper;

    /** 解析接口的两种阶段定位方式，限制在指定作业内。 */
    public AutoexecJobPhaseVo phase(JSONObject request, Long jobId) {
        Long id = request.getLong("jobPhaseId");
        if (id == null) { id = request.getLong("phaseId"); }
        if (id != null) {
            AutoexecJobPhaseVo phase = jobMapper.getJobPhaseByPhaseId(id);
            return phase != null && (jobId == null || Objects.equals(jobId, phase.getJobId())) ? phase : null;
        }
        if (jobId != null && request.getString("phaseName") != null) {
            for (AutoexecJobPhaseVo phase : jobMapper.getJobPhaseListByJobId(jobId)) {
                if (Objects.equals(phase.getName(), request.getString("phaseName"))) { return phase; }
            }
        }
        return null;
    }

    /** 按实际对象类型决定列表展示层级，本地 Runner 操作属于阶段。 */
    public String objectType(JobAction action, AutoexecJobPhaseVo phase, JSONObject request) {
        if ("job".equals(action.getAuditObjectType()) || "phase".equals(action.getAuditObjectType())) { return action.getAuditObjectType(); }
        // 显式重置或忽略全部目标属于整个阶段；选中批量仍保留节点或 SQL 文件粒度。
        if (phase != null && request.getIntValue("isAll") == 1
                && (action == JobAction.RESET_NODE || action == JobAction.RESET_SQL || action == JobAction.IGNORE_NODE)) {
            return "phase";
        }
        if (phase != null && ExecMode.SQL.getValue().equals(phase.getExecMode())) { return "sql"; }
        if (phase != null && ExecMode.RUNNER.getValue().equals(phase.getExecMode())) { return "phase"; }
        return action.getAuditObjectType();
    }

    /** 读取目标身份与名称快照，SQL 通过来源契约分页获取，不采集业务状态。 */
    public List<AutoexecJobOperationAuditTargetVo> targets(AutoexecJobVo job, AutoexecJobPhaseVo phase,
            String objectType, JSONObject request) {
        List<AutoexecJobOperationAuditTargetVo> targets = new ArrayList<>();
        if ("job".equals(objectType)) {
            AutoexecJobOperationAuditTargetVo target = new AutoexecJobOperationAuditTargetVo();
            target.setObjectId(job.getId());
            target.setNodeName(job.getName()); targets.add(target);
        } else if (phase != null && "phase".equals(objectType)) {
            AutoexecJobOperationAuditTargetVo target = target(phase);
            target.setObjectId(phase.getId());
            targets.add(target);
        } else if (phase != null && "sql".equals(objectType)) {
            IAutoexecJobSourceTypeHandler handler = sqlSource(job);
            if (handler != null) {
                AutoexecJobPhaseNodeVo search = new AutoexecJobPhaseNodeVo();
                search.setJobId(job.getId()); search.setJobPhaseId(phase.getId()); search.setPageSize(200); search.setNeedPage(true);
                for (int page = 1; ; page++) {
                    search.setCurrentPage(page);
                    JSONObject result = handler.getOperationAuditSqlPage(search);
                    JSONArray rows = JSON.parseObject(JSON.toJSONString(result)).getJSONArray("tbodyList");
                    if (rows == null || rows.isEmpty()) { break; }
                    for (int i = 0; i < rows.size(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        if (!selected(request, row.getLong("id"), row.getLong("resourceId"), row.getString("sqlFile"), true)) { continue; }
                        AutoexecJobOperationAuditTargetVo target = target(phase);
                        target.setObjectId(row.getLong("id"));
                        target.setResourceId(row.getLong("resourceId")); target.setNodeName(row.getString("nodeName"));
                        target.setHost(row.getString("host")); target.setSqlFile(row.getString("sqlFile"));
                        targets.add(target);
                    }
                    if (rows.size() < 200) { break; }
                }
            }
        } else if (phase != null) {
            for (AutoexecJobPhaseNodeVo node : jobMapper.getJobPhaseNodeListByJobIdAndPhaseId(job.getId(), phase.getId())) {
                if (!selected(request, node.getId(), node.getResourceId(), null, false)) { continue; }
                AutoexecJobOperationAuditTargetVo target = target(phase);
                target.setObjectId(node.getId()); target.setResourceId(node.getResourceId());
                target.setNodeName(node.getNodeName()); target.setHost(node.getHost());
                targets.add(target);
            }
        }
        return targets;
    }

    /** 通过已有来源契约解析 SQL 查询能力，基础模块不引用发布实现。 */
    protected IAutoexecJobSourceTypeHandler sqlSource(AutoexecJobVo job) {
        IAutoexecJobSource source = AutoexecJobSourceFactory.getEnumInstance(job.getSource());
        return source == null ? null : AutoexecJobSourceTypeHandlerFactory.getAction(source.getType());
    }

    /** 只匹配请求指定的对象；全部操作由显式 isAll 标志决定。 */
    private boolean selected(JSONObject request, Long id, Long resourceId, String file, boolean sql) {
        if (request.getIntValue("isAll") == 1) { return true; }
        JSONArray ids = request.getJSONArray(sql ? "sqlIdList" : "resourceIdList");
        if (ids != null && !ids.isEmpty()) { return ids.toJavaList(Long.class).contains(sql ? id : resourceId); }
        if (request.getString("sqlName") != null && !Objects.equals(request.getString("sqlName"), file)) { return false; }
        if (request.getLong("resourceId") != null) { return Objects.equals(request.getLong("resourceId"), resourceId); }
        if (request.getLong("nodeId") != null && !sql) { return Objects.equals(request.getLong("nodeId"), id); }
        return sql && request.getString("sqlName") != null && resourceId == null;
    }

    /** 创建包含阶段标识的基础快照。 */
    private AutoexecJobOperationAuditTargetVo target(AutoexecJobPhaseVo phase) {
        AutoexecJobOperationAuditTargetVo target = new AutoexecJobOperationAuditTargetVo();
        target.setPhaseId(phase.getId()); target.setPhaseName(phase.getName());
        return target;
    }
}
