package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/28 10:14 上午
 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseSqlSearchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "作业剧本sql文件节点搜索";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/sql/search";
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "作业剧本id", isRequired = true),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "sql文件状态"),
            @Param(name = "isDelete", type = ApiParamType.INTEGER, desc = "是否删除"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词(节点名称或ip)", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "sql文件列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo jobPhaseNodeVo = JSONObject.toJavaObject(paramObj, AutoexecJobPhaseNodeVo.class);

        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(paramObj.getLong("jobPhaseId"));
        if (phaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(paramObj.getLong("jobPhaseId").toString());
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(phaseVo.getJobId().toString());
        }

//        if (StringUtils.equals(jobVo.getSource(), JobSource.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            int sqlCount = iDeploySqlCrossoverMapper.searchDeploySqlCount(jobPhaseNodeVo);
            if (sqlCount > 0) {
                jobPhaseNodeVo.setRowNum(sqlCount);
                result = TableResultUtil.getResult(iDeploySqlCrossoverMapper.searchDeploySql(jobPhaseNodeVo), jobPhaseNodeVo);
            }
//        } else {
//            int sqlCount = autoexecJobMapper.searchJobPhaseSqlCount(jobPhaseNodeVo);
//            if (sqlCount > 0) {
//                jobPhaseNodeVo.setRowNum(sqlCount);
//                result = TableResultUtil.getResult(autoexecJobMapper.searchJobPhaseSql(jobPhaseNodeVo), jobPhaseNodeVo);
//            }
//        }
        return result;
    }
}
