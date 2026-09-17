package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import org.springframework.stereotype.Service;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;

/** 按作业解析 SQL 阶段，委托来源处理器重置文件状态。 */
@Service
public class AutoexecJobSqlResetHandler extends AutoexecJobActionHandlerBase {

    @Override
    public String getName() { return JobAction.RESET_SQL.getValue(); }

    /** 执行原有业务逻辑，采集由公共 doService 统一负责。 */
    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        JSONObject paramObj = jobVo.getActionParam();
        // 来源处理器依赖已解析的阶段，按当前作业的阶段名定位，避免空阶段或跨作业处理。
        AutoexecJobPhaseVo phase = autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()).stream()
                .filter(item -> item.getName().equals(paramObj.getString("phaseName"))).findFirst().orElse(null);
        if (phase == null) { throw new AutoexecJobPhaseNotFoundException(String.valueOf(paramObj.getString("phaseName"))); }
        jobVo.setExecutePhase(phase);
        IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
        if (jobSource == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
        autoexecJobSourceActionHandler.resetSqlStatus(paramObj, jobVo);
        return null;
    }
}
