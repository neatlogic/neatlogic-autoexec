package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/5/18 2:25 下午
 */
@Service
public class AutoexecJobPhaseSqlStatusResetApi extends PrivateApiComponentBase {

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
            @Param(name = "sqlIdList", type = ApiParamType.JSONARRAY, desc = "sql文件列表")
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(paramObj.getLong("jobId"));
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(paramObj.getLong("jobId"));
        }
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            if (StringUtils.equals(JobSource.DEPLOY.getValue(), jobVo.getSource())) {
                IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
                iDeploySqlCrossoverMapper.resetDeploySqlStatusBySqlIdList(sqlIdArray);
            }else {
                autoexecJobMapper.resetJobSqlStatusBySqlIdList(sqlIdArray);}
        }
        return null;
    }
}
