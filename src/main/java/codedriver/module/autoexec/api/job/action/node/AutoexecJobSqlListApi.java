package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.constvalue.AutoexecJobSqlOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobSqlDetailVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/4/25 6:33 下午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobSqlListApi extends PublicApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取作业执行sql文件状态列表";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sqlVoList", type = ApiParamType.JSONARRAY, desc = "自动化sql文件列表"),
            @Param(name = "operType", type = ApiParamType.STRING, isRequired = true, desc = "标记")
    })
    @Output({
    })
    @Description(desc = "获取作业执行sql文件状态列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray SqlVoArray = paramObj.getJSONArray("sqlVoList");
        if (CollectionUtils.isNotEmpty(SqlVoArray)) {
            if (StringUtils.equals(paramObj.getString("operType"), AutoexecJobSqlOperType.AUTO.getValue())) {
                List<AutoexecJobSqlDetailVo> sqlVoList = SqlVoArray.toJavaList(AutoexecJobSqlDetailVo.class);
                return autoexecJobMapper.getJobSqlDetailList(sqlVoList);
            } else if (StringUtils.equals(paramObj.getString("operType"), AutoexecJobSqlOperType.DEPLOY.getValue())) {
                IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
                List<DeploySqlDetailVo> sqlVoList = SqlVoArray.toJavaList(DeploySqlDetailVo.class);
                return iDeploySqlCrossoverMapper.getJobDeploySqlDetailList(sqlVoList);
            }
        }
        return null;
    }
}
