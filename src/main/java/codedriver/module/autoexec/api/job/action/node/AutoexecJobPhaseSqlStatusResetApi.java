package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
        return "autoexec/job/sql/status/reset";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "jobPhaseName", type = ApiParamType.STRING, desc = "作业剧本名称"),
            @Param(name = "sqlFileList", type = ApiParamType.JSONARRAY, desc = "sql文件列表"),
            @Param(name = "jobPhaseNameList", type = ApiParamType.JSONARRAY, desc = "作业剧本名称列表")
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        //两种调用情景：
        //1、当前作业下的某个阶段的sql文件批量重置  入参：jobId、jobPhaseName、sqlFileList
        //2、当前作业的多个阶段重置               入参：jobId、jonPhaseNameList
        Long jobId = paramObj.getLong("jobId");
        JSONArray sqlFileArray = paramObj.getJSONArray("sqlFileList");
        if (CollectionUtils.isNotEmpty(sqlFileArray)) {
            //1、当前作业下的某个阶段的sql文件批量重置
            String jobPhaseName = paramObj.getString("jobPhaseName");
            if (StringUtils.isEmpty(jobPhaseName)) {
                throw new ParamNotExistsException("jobPhaseName");
            }
            autoexecService.resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameAndSqlFileList(jobId,jobPhaseName, sqlFileArray.toJavaList(String.class));
        } else {
            JSONArray jonPhaseNameArray = paramObj.getJSONArray("jobPhaseNameList");
            if (CollectionUtils.isEmpty(jonPhaseNameArray)) {
                throw new ParamNotExistsException("jobPhaseNameList");
            }
            //2、当前作业的多个阶段重置
            autoexecService.resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameList(paramObj.getLong("jobId"), jonPhaseNameArray.toJavaList(String.class));
        }
        return null;
    }
}
