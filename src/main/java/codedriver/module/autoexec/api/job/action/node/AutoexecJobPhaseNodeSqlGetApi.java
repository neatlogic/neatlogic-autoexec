package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/26 10:12 上午
 */
@Service
public class AutoexecJobPhaseNodeSqlGetApi extends PublicApiComponentBase {


    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取作业阶段节点执行sql状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/sql/get";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业 id"),
            @Param(name = "nodeId", type = ApiParamType.LONG, isRequired = true, desc = "节点 id"),
            @Param(name = "sqlFile", type = ApiParamType.STRING, isRequired = true, desc = "sql文件名"),
    })
    @Output({
    })
    @Description(desc = "更新作业阶段节点执行sql状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return null;
    }
}
