package neatlogic.module.autoexec.service;

import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecJobOperationAuditMapper;

/** 只访问审计表；写入使用独立事务，删除随作业事务一起提交或回滚。 */
@Service
public class AutoexecJobOperationAuditStore {
    @Resource private AutoexecJobOperationAuditMapper mapper;

    /** 加入作业删除事务，先删明细再删主记录，失败时由调用方整体回滚。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByJobId(Long jobId) {
        mapper.deleteTargetsByJobId(jobId);
        mapper.deleteAuditsByJobId(jobId);
    }

    /** 保存初始快照，分批写入避免超过数据库报文大小。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void begin(AutoexecJobOperationAuditVo audit, List<AutoexecJobOperationAuditTargetVo> targets) {
        mapper.insertAudit(audit);
        for (int i = 0; i < targets.size(); i += 200) {
            mapper.insertTargets(targets.subList(i, Math.min(i + 200, targets.size())));
        }
    }

    /** 独立保存接管及人工交互上下文，不维护业务结果。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void supplement(AutoexecJobOperationAuditVo audit) {
        mapper.updateContext(audit);
    }
}
