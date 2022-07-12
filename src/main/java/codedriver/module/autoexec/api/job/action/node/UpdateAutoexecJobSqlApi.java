package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerFactory;
import codedriver.framework.autoexec.job.source.action.IAutoexecJobSourceActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/26 10:12 上午
 */
@Service
@Transactional
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
        if (autoexecJobMapper.getJobInfo(paramObj.getLong("jobId")) == null) {
            throw new AutoexecJobNotFoundException(paramObj.getLong("jobId"));
        }
        IAutoexecJobSourceActionHandler handler;
        if (StringUtils.equals(paramObj.getString("operType"), codedriver.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue())) {
            handler = AutoexecJobSourceActionHandlerFactory.getAction(codedriver.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue());
            handler.updateSqlStatus(paramObj);
        } else if (StringUtils.equals(paramObj.getString("operType"), JobSourceType.DEPLOY.getValue())) {
            handler = AutoexecJobSourceActionHandlerFactory.getAction(JobSourceType.DEPLOY.getValue());
            handler.updateSqlStatus(paramObj);
        }
        return null;
    }
}
