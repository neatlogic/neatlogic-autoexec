package neatlogic.module.autoexec.api.job.action.node;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.AutoexecJobSourceVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
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
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
        autoexecJobSourceActionHandler.resetSqlStatus(paramObj, jobVo);
        return null;
    }
}
