package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
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
public class AutoexecJobSqlUpdateApi extends PublicApiComponentBase {

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
            @Param(name = "phaseName", type = ApiParamType.STRING, isRequired = true, desc = "作业剧本名"),
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
        if (StringUtils.equals(paramObj.getString("operType"), AutoexecOperType.AUTOEXEC.getValue())) {
            AutoexecSqlDetailVo paramSqlVo = new AutoexecSqlDetailVo(paramObj.getJSONObject("sqlStatus"));
            paramSqlVo.setPhaseName(paramObj.getString("phaseName"));
            if (autoexecJobMapper.updateSqlDetailIsDeleteAndStatusAndMd5AndLcd(paramSqlVo) == 0) {
                autoexecJobMapper.insertSqlDetail(paramSqlVo);
            }
        } else if (StringUtils.equals(paramObj.getString("operType"), DeployOperType.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            DeploySqlDetailVo paramDeploySqlVo = new DeploySqlDetailVo(paramObj.getJSONObject("sqlStatus"));
            paramDeploySqlVo.setRunnerId(paramObj.getLong("runnerId"));
            DeploySqlDetailVo oldDeploySqlVo = iDeploySqlCrossoverMapper.getDeploySqlBySysIdAndModuleIdAndEnvIdAndVersionAndSqlFile(paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramDeploySqlVo.getSqlFile());
            if (oldDeploySqlVo != null) {
                iDeploySqlCrossoverMapper.updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(paramDeploySqlVo.getStatus(), paramDeploySqlVo.getMd5(), oldDeploySqlVo.getId());
            } else {
                iDeploySqlCrossoverMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("phaseName"), paramDeploySqlVo.getId()));
                iDeploySqlCrossoverMapper.insertDeploySqlDetail(paramDeploySqlVo, paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramObj.getLong("runnerId"));
            }
        }
        return null;
    }
}
