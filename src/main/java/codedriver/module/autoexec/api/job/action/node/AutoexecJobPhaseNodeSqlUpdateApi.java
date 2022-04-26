package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeSqlVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/25 6:33 下午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobPhaseNodeSqlUpdateApi extends PublicApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "更新作业阶段节点执行sql状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/sql/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业 id"),
            @Param(name = "nodeId", type = ApiParamType.LONG, isRequired = true, desc = "节点 id"),
            @Param(name = "nodeName", type = ApiParamType.STRING, desc = "节点名称"),
            @Param(name = "sqlFile", type = ApiParamType.STRING, isRequired = true, desc = "sql文件名"),
            @Param(name = "host", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态"),
            @Param(name = "accessEndpoint", type = ApiParamType.STRING, desc = "访问入口"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id")
    })
    @Output({
    })
    @Description(desc = "更新作业阶段节点执行sql状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobPhaseNodeSqlVo phaseNodeSqlVo = paramObj.toJavaObject(AutoexecJobPhaseNodeSqlVo.class);
        autoexecJobMapper.updateJobPhaseNodeSql(phaseNodeSqlVo);
        return null;
    }
}
