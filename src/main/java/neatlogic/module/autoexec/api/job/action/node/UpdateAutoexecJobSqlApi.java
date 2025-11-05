package neatlogic.module.autoexec.api.job.action.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/26 10:12 上午
 */
@Service
@SystemUser("autoexec")
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobSqlApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "更新作业执行sql文件状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业 id"),
            @Param(name = "phaseName", type = ApiParamType.STRING, isRequired = true, desc = "作业剧本名（执行sql）"),
            @Param(name = "sqlStatus", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "sql状态"),
            @Param(name = "operType", type = ApiParamType.ENUM, rule = "auto,deploy", isRequired = true, desc = "来源类型")
    })
    @Output({
    })
    @Description(desc = "更新作业执行sql文件状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        if (autoexecJobMapper.getJobLockByJobId(paramObj.getLong("jobId")) == null) {
            throw new AutoexecJobNotFoundException(paramObj.getLong("jobId"));
        }
        IAutoexecJobSourceTypeHandler handler = AutoexecJobSourceTypeHandlerFactory.getAction(paramObj.getString("operType"));
        if(handler != null) {
            handler.updateSqlStatus(paramObj);
        }
        return null;
    }
}
