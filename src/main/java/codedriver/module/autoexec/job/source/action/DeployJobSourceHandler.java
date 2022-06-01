package codedriver.module.autoexec.job.source.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerBase;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/5/31 5:22 下午
 */
@Service
public class DeployJobSourceHandler extends AutoexecJobSourceActionHandlerBase {

    @Override
    public String getName() {
        return DeployOperType.DEPLOY.getValue();
    }

    @Override
    public void saveJobPhase(AutoexecCombopPhaseVo combopPhaseVo) {

    }

    @Override
    public String getJobSqlContent(AutoexecJobVo jobVo) {
        JSONObject paramObj = jobVo.getActionParam();
        IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
        DeploySqlDetailVo sqlDetailVo = iDeploySqlCrossoverMapper.getJobSqlDetailById(paramObj.getLong("sqlId"));
        paramObj.put("sysId", sqlDetailVo.getSysId());
        paramObj.put("moduleId", sqlDetailVo.getModuleId());
        paramObj.put("envId", sqlDetailVo.getEnvId());
        paramObj.put("version", sqlDetailVo.getVersion());
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(sqlDetailVo.getEnvId());
        paramObj.put("envName", ciEntityVo.getName());
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        return requestRunner(nodeVo.getRunnerUrl() + "/api/rest/deploy/sql/content/get", paramObj);
    }

    @Override
    public void downloadJobSqlFile(AutoexecJobVo jobVo) throws Exception {
        JSONObject paramObj = jobVo.getActionParam();
        IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
        DeploySqlDetailVo sqlDetailVo = iDeploySqlCrossoverMapper.getJobSqlDetailById(paramObj.getLong("sqlId"));
        paramObj.put("sysId", sqlDetailVo.getSysId());
        paramObj.put("moduleId", sqlDetailVo.getModuleId());
        paramObj.put("envId", sqlDetailVo.getEnvId());
        paramObj.put("version", sqlDetailVo.getVersion());
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(sqlDetailVo.getEnvId());
        paramObj.put("envName", ciEntityVo.getName());
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        String url = nodeVo.getRunnerUrl() + "/api/binary/deploy/sql/file/download";
        String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream()).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest().getError();

        if (StringUtils.isNotBlank(result)) {
            throw new AutoexecJobRunnerHttpRequestException(url + ":" + result);
        }
    }
}
