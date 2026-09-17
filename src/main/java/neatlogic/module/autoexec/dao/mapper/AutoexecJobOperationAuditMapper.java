package neatlogic.module.autoexec.dao.mapper;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;

/** 操作审计的存储与只读检索，不访问业务行锁。 */
public interface AutoexecJobOperationAuditMapper {
    /** 按作业归属删除目标明细，必须先于主记录删除。 */
    void deleteTargetsByJobId(@Param("jobId") Long jobId);
    /** 删除指定作业的操作主记录。 */
    void deleteAuditsByJobId(@Param("jobId") Long jobId);
    /** 写入独立主记录。 */
    void insertAudit(AutoexecJobOperationAuditVo audit);
    /** 批量写入白名单目标快照。 */
    void insertTargets(@Param("list") List<AutoexecJobOperationAuditTargetVo> targets);
    /** 补充接管和人工交互上下文。 */
    void updateContext(AutoexecJobOperationAuditVo audit);
    /** 统计满足组合条件的记录。 */
    int countAudits(JSONObject search);
    /** 按时间和 ID 倒序分页。 */
    List<AutoexecJobOperationAuditVo> searchAudits(JSONObject search);
    /** 同时约束作业与记录 ID。 */
    AutoexecJobOperationAuditVo getAudit(@Param("jobId") Long jobId, @Param("id") Long id);
    /** 分页获取记录目标。 */
    List<AutoexecJobOperationAuditTargetVo> getTargets(@Param("auditId") Long auditId, @Param("startNum") int startNum, @Param("pageSize") int pageSize);
}
