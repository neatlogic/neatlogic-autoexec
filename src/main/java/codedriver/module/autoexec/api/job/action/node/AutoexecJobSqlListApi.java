package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
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
            @Param(name = "sqlInfoList", type = ApiParamType.JSONARRAY, desc = "sql列表"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "作业剧本名"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "version", type = ApiParamType.STRING, desc = "版本"),
            @Param(name = "operType", type = ApiParamType.ENUM, rule = "auto,deploy", isRequired = true, desc = "来源类型")
    })
    @Output({
    })
    @Description(desc = "获取作业执行sql文件状态列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        if (StringUtils.equals(paramObj.getString("operType"), AutoexecOperType.AUTOEXEC.getValue())) {
            JSONArray SqlVoArray = paramObj.getJSONArray("sqlInfoList");
            if (CollectionUtils.isNotEmpty(SqlVoArray)) {
                List<AutoexecSqlDetailVo> sqlDetailVoList = SqlVoArray.toJavaList(AutoexecSqlDetailVo.class);
                return autoexecJobMapper.getJobSqlDetailList(sqlDetailVoList, paramObj.getString("phaseName"));
            }
        } else if (StringUtils.equals(paramObj.getString("operType"), DeployOperType.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            List<DeploySqlDetailVo> sqlDetailVoList = new ArrayList<>();
            sqlDetailVoList.add(paramObj.toJavaObject(DeploySqlDetailVo.class));
            return iDeploySqlCrossoverMapper.getDeploySqlDetailList(sqlDetailVoList);
        }
        return null;
    }
}
