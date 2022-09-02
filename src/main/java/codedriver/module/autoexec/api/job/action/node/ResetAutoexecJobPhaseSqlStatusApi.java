package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/5/18 2:25 下午
 */
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Service
public class ResetAutoexecJobPhaseSqlStatusApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "重置sql文件状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/status/reset";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "sqlIdList", type = ApiParamType.JSONARRAY, desc = "sql文件列表"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "剧本名称"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, desc = "是否全部重置,1:是 0:否,则sqlIdList不能为空"),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(paramObj.getLong("jobId"));
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(paramObj.getLong("jobId"));
        }
        IAutoexecJobSourceTypeHandler handler;
        if (StringUtils.equals(JobSource.DEPLOY.getValue(), jobVo.getSource())) {
            handler = AutoexecJobSourceTypeHandlerFactory.getAction(JobSourceType.DEPLOY.getValue());
            handler.resetSqlStatus(paramObj, jobVo);
        } else {
            handler = AutoexecJobSourceTypeHandlerFactory.getAction(codedriver.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue());
            handler.resetSqlStatus(paramObj, jobVo);
        }
        return null;
    }
}
