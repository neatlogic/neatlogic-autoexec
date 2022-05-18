package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/5/18 2:25 下午
 */
@Service
public class AutoexecJobPhaseSqlStatusResetApi extends PrivateApiComponentBase {

    @Resource
    AutoexecService autoexecService;

    @Override
    public String getName() {
        return "重置sql文件状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/reset";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "jobPhaseNameList", type = ApiParamType.JSONARRAY, desc = "作业剧本名称列表", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        autoexecService.resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameList(paramObj.getLong("jobId"), paramObj.getJSONArray("jobPhaseNameList").toJavaList(String.class));
        return null;
    }
}
