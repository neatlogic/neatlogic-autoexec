package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobSqlDetailVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeployJobSqlVo;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
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
import java.util.Date;

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
        return "添加作业执行sql文件状态";
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
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "节点 id"),
            @Param(name = "sqlFile", type = ApiParamType.STRING, isRequired = true, desc = "sql文件名"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id"),
            @Param(name = "host", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "status", type = ApiParamType.ENUM, isRequired = true, rule = "pending,running,aborting,aborted,succeed,failed,ignored,waitInput", desc = "状态"),
            @Param(name = "operType", type = ApiParamType.STRING, isRequired = true, desc = "标记")
    })
    @Output({
    })
    @Description(desc = "添加作业执行sql文件状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        if (autoexecJobMapper.getJobInfo(paramObj.getLong("jobId")) == null) {
            throw new AutoexecJobNotFoundException(paramObj.getLong("jobId"));
        }
        if (StringUtils.equals(paramObj.getString("operType"), AutoexecOperType.AUTOEXEC.getValue())) {
            AutoexecJobSqlDetailVo paramSqlVo = new AutoexecJobSqlDetailVo(paramObj);
            AutoexecJobSqlDetailVo oldSqlVo = autoexecJobMapper.getJobSqlDetailByJobIdAndNodeIdAndSqlFile(paramSqlVo.getJobId(), paramSqlVo.getNodeId(), paramSqlVo.getSqlFile());
            if (oldSqlVo != null) {
                autoexecJobMapper.updateJobSqlDetailIsDeleteAndStatusAndMd5AndLcdById(paramSqlVo.getStatus(), paramSqlVo.getMd5(), oldSqlVo.getId());
            } else {
                paramSqlVo.setLcd(new Date());
                autoexecJobMapper.insertJobSqlDetail(paramSqlVo);
            }
        } else if (StringUtils.equals(paramObj.getString("operType"), DeployOperType.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            //TODO 发起作业时还要补 插入发布作业的执行sql文件 的逻辑
            DeploySqlDetailVo paramDeploySqlVo = new DeploySqlDetailVo(paramObj);
            DeploySqlDetailVo oldDeploySqlVo = iDeploySqlCrossoverMapper.getAutoexecJobIdByDeploySqlDetailVo(paramDeploySqlVo);
            if (oldDeploySqlVo != null) {
                iDeploySqlCrossoverMapper.updateJobDeploySqlDetailIsDeleteAndStatusAndMd5AndLcdById(paramDeploySqlVo.getStatus(), paramDeploySqlVo.getMd5(), oldDeploySqlVo.getId());
            } else {
                iDeploySqlCrossoverMapper.insertDeploySql(new DeployJobSqlVo(paramObj.getLong("jobId"), paramDeploySqlVo.getId()));
                iDeploySqlCrossoverMapper.insertDeploySqlDetail(paramDeploySqlVo);
            }
        }
        return null;
    }
}
