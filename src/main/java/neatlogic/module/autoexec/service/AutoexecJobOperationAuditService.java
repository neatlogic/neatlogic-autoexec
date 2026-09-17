package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;
import neatlogic.framework.autoexec.job.audit.JobOperationAuditContext;
import neatlogic.framework.autoexec.job.audit.IJobOperationAuditService;
import neatlogic.framework.util.SnowflakeUtil;

/** 统一管理请求级审计生命周期，业务入口不承担存储、去重和事务处理。 */
@Service
public class AutoexecJobOperationAuditService implements IJobOperationAuditService {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobOperationAuditService.class);
    @Resource private AutoexecJobMapper jobMapper;
    @Resource private AutoexecJobOperationAuditStore store;
    @Resource private AutoexecJobOperationAuditSnapshotService snapshots;

    /** 最外层白名单动作创建记录，嵌套调用复用上下文。 */
    @Override
    public <T> T execute(JobAction action, JSONObject request, Operation<T> operation) throws Exception {
        if (!action.isAuditAction() || JobOperationAuditContext.current() != null) { return operation.execute(); }
        JobOperationAuditContext context = JobOperationAuditContext.open();
        try {
            return capture(action, request, operation, context);
        } finally {
            JobOperationAuditContext.close();
        }
    }

    /** 在最外层动作执行期间保存快照与上下文，不跟踪执行结果。 */
    private <T> T capture(JobAction action, JSONObject request, Operation<T> operation, JobOperationAuditContext context) throws Exception {
        AutoexecJobOperationAuditVo audit = null;
        List<AutoexecJobOperationAuditTargetVo> targets = new ArrayList<>();
        boolean persisted = false;
        try {
            AutoexecJobPhaseVo phase = snapshots.phase(request, request.getLong("jobId"));
            Long jobId = request.getLong("jobId");
            if (jobId == null && phase != null) { jobId = phase.getJobId(); }
            AutoexecJobVo job = jobId == null ? null : jobMapper.getJobInfo(jobId);
            if (job != null) {
                audit = begin(action, request, job, phase, targets);
                store.begin(audit, targets);
                persisted = true;
            }
        } catch (Exception ex) {
            // 审计不可用不得阻断原业务；不输出请求内容和凭据。
            logger.error("Unable to begin job operation audit, action={}", action.getValue(), ex);
        }
        try {
            T result = operation.execute();
            if (persisted && action == JobAction.TAKE_OVER) {
                audit.setCurrentExecUser(audit.getOperatorUuid());
            }
            return result;
        } catch (Exception ex) {
            // 原异常直接透传，操作记录不保存业务结果或异常正文。
            logger.error("Job operation failed, auditId={}, action={}", audit == null ? null : audit.getId(), action.getValue(), ex);
            throw ex;
        } finally {
            if (persisted) {
                audit.setInteractionType(context.getInteractionType());
                audit.setInteractionValue(context.getInteractionValue());
                // 只补充业务入口产生的上下文，不跟踪提交、回滚或异步执行结果。
                if (audit.getCurrentExecUser() != null || audit.getInteractionType() != null) {
                    supplementSafely(audit);
                }
            }
        }
    }

    /** 持久化展示字段有界，避免超长名称导致整条审计丢失。 */
    private String limit(String value, int length) { return value == null || value.length() <= length ? value : value.substring(0, length); }

    /** 在执行前保存定位快照和允许的策略字段。 */
    private AutoexecJobOperationAuditVo begin(JobAction action, JSONObject request, AutoexecJobVo job,
            AutoexecJobPhaseVo phase, List<AutoexecJobOperationAuditTargetVo> targets) {
        AutoexecJobOperationAuditVo audit = new AutoexecJobOperationAuditVo();
        audit.setId(SnowflakeUtil.uniqueLong()); audit.setJobId(job.getId());
        audit.setOperateTime(System.currentTimeMillis()); audit.setOperatorUuid(UserContext.get().getUserUuid());
        audit.setAction(action.getValue());
        audit.setObjectType(snapshots.objectType(action, phase, request));
        audit.setTargetName(phase == null ? job.getName() : phase.getName());
        if (action == JobAction.TAKE_OVER) { audit.setPreviousExecUser(job.getExecUser()); }
        if (action == JobAction.REFIRE || action == JobAction.REFIRE_PHASE) {
            String strategy = request.getString("type");
            audit.setStrategy("refireAll".equals(strategy) ? "refireAll" : "refireResetAll");
        }
        targets.addAll(snapshots.targets(job, phase, audit.getObjectType(), request));
        audit.setTargetCount(targets.size());
        if (!targets.isEmpty() && !"job".equals(audit.getObjectType()) && !"phase".equals(audit.getObjectType())) {
            AutoexecJobOperationAuditTargetVo first = targets.get(0);
            String nodeName = first.getNodeName() != null ? first.getNodeName() : first.getHost() != null ? first.getHost() : String.valueOf(first.getObjectId());
            String path = first.getPhaseName() + " / " + nodeName;
            if (first.getSqlFile() != null) { path += " / " + first.getSqlFile(); }
            audit.setTargetName(path.length() > 1000 ? path.substring(0, 1000) : path);
        }
        for (AutoexecJobOperationAuditTargetVo target : targets) {
            target.setId(SnowflakeUtil.uniqueLong()); target.setAuditId(audit.getId());
            target.setPhaseName(limit(target.getPhaseName(), 255)); target.setNodeName(limit(target.getNodeName(), 255));
            target.setSqlFile(limit(target.getSqlFile(), 1024));
        }
        return audit;
    }

    /** 上下文补充失败仅记录日志，不改变业务原始返回或触发重试。 */
    private void supplementSafely(AutoexecJobOperationAuditVo audit) {
        try { store.supplement(audit); }
        catch (Exception ex) { logger.error("Unable to supplement job operation audit, auditId={}", audit.getId(), ex); }
    }
}
