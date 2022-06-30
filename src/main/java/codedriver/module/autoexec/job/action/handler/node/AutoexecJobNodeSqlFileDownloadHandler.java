package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerFactory;
import codedriver.framework.autoexec.job.source.action.IAutoexecJobSourceActionHandler;
import codedriver.framework.deploy.constvalue.JobSourceType;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
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
            if (StringUtils.equals(jonInfo.getSource(), JobSourceType.DEPLOY.getValue())) {
                IAutoexecJobSourceActionHandler jobSourceActionHandler = AutoexecJobSourceActionHandlerFactory.getAction(JobSourceType.DEPLOY.getValue());
                jobSourceActionHandler.downloadJobSqlFile(jobVo);
            } else {
                IAutoexecJobSourceActionHandler jobSourceActionHandler = AutoexecJobSourceActionHandlerFactory.getAction(codedriver.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue());
                jobSourceActionHandler.downloadJobSqlFile(jobVo);
            }
        }
        return null;
    }
}
