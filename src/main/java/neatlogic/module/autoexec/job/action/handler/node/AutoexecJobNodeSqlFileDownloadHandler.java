package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/5/30 10:57 上午
 */
@Service
public class AutoexecJobNodeSqlFileDownloadHandler extends AutoexecJobActionHandlerBase {

    @Override
    public String getName() {
        return JobAction.DOWNLOAD_NODE_SQL_FILE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        AutoexecJobVo jonInfo = autoexecJobMapper.getJobInfo(jobVo.getCurrentNode().getJobId());
        if (jonInfo != null) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jonInfo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler jobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            jobSourceActionHandler.downloadJobSqlFile(jobVo);
        }
        return null;
    }
}

