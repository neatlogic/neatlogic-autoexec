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
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

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
            @Param(name = "sqlIdList", type = ApiParamType.JSONARRAY, desc = "sql文件列表"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "剧本名称"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, desc = "是否全部重置,1:是 0:否,则sqlIdList不能为空"),
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

        if (StringUtils.equals(JobSource.DEPLOY.getValue(), jobVo.getSource())) {
            //发布
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            if (!Objects.isNull(paramObj.getInteger("isAll")) && paramObj.getInteger("isAll") == 1) {
                //重置phase的所有sql文件状态
                List<Long> deleteSqlIdList = iDeploySqlCrossoverMapper.getJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"),paramObj.getString("phaseName") );
                if (CollectionUtils.isNotEmpty(deleteSqlIdList)) {
                    iDeploySqlCrossoverMapper.resetDeploySqlStatusBySqlIdList(deleteSqlIdList);
                }
            } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
                //批量重置sql文件状态
                iDeploySqlCrossoverMapper.resetDeploySqlStatusBySqlIdList(sqlIdArray.toJavaList(Long.class));
            }
        } else {
            //自动化
            if (!Objects.isNull(paramObj.getInteger("isAll")) && paramObj.getInteger("isAll") == 1) {
                //重置phase的所有sql文件状态
                List<Long> deleteSqlIdList = autoexecJobMapper.getJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
                if (CollectionUtils.isNotEmpty(deleteSqlIdList)) {
                    autoexecJobMapper.resetJobSqlStatusBySqlIdList(deleteSqlIdList);
                }
            } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
                //批量重置sql文件状态
                autoexecJobMapper.resetJobSqlStatusBySqlIdList(sqlIdArray.toJavaList(Long.class));
            }
        }
        return null;
    }
}
