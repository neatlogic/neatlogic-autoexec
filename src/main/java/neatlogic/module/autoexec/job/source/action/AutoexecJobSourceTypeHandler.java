package neatlogic.module.autoexec.job.source.action;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.ISqlNodeDetail;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.*;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.framework.common.util.IpUtil;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.GroupNetworkVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/5/31 2:34 下午
 */
@Service
public class AutoexecJobSourceTypeHandler extends AutoexecJobSourceTypeHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return JobSourceType.AUTOEXEC.getValue();
    }

    @Override
    public void saveJobPhase(AutoexecCombopPhaseVo combopPhaseVo) {

    }

    @Override
    public JSONObject getJobSqlContent(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        return JSONObject.parseObject(AutoexecUtil.requestRunner(nodeVo.getRunnerUrl() + "/api/rest/job/phase/node/sql/content/get", paramObj));
    }

    @Override
    public void downloadJobSqlFile(AutoexecJobVo jobVo) throws Exception {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + paramObj.getString("sqlName") + "\"");
        String url = nodeVo.getRunnerUrl() + "/api/binary/job/phase/node/sql/file/download";
        String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream()).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest().getError();

        if (StringUtils.isNotBlank(result)) {
            throw new RunnerHttpRequestException(url + ":" + result);
        }
    }

    public List<Long> getSqlIdsAndExecuteJobNodes(JSONObject paramObj, AutoexecJobVo jobVo) {
        List<AutoexecSqlNodeDetailVo> sqlDetailList = null;
        List<Long> sqlIdList = null;
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        if (paramObj.getInteger("isAll") != null && paramObj.getInteger("isAll") == 1) {
            //重置phase的所有sql文件状态
            sqlDetailList = autoexecJobMapper.getJobSqlListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            sqlIdList = sqlDetailList.stream().map(AutoexecSqlNodeDetailVo::getId).collect(Collectors.toList());
            List<AutoexecJobPhaseNodeVo> jobPhaseNodeVos = new ArrayList<>();
            List<RunnerMapVo> runnerMapVos = runnerMapper.getRunnerByRunnerMapIdList(sqlDetailList.stream().map(AutoexecSqlNodeDetailVo::getRunnerId).collect(Collectors.toList()));
            for (AutoexecSqlNodeDetailVo resetSql : sqlDetailList) {
                Optional<RunnerMapVo> runnerMapVoOptional = runnerMapVos.stream().filter(o -> Objects.equals(o.getRunnerMapId(), resetSql.getRunnerId())).findFirst();
                if (!runnerMapVoOptional.isPresent()) {
                    throw new AutoexecJobRunnerNotFoundException(resetSql.getRunnerId().toString());
                }
                jobPhaseNodeVos.add(new AutoexecJobPhaseNodeVo(resetSql.getJobId(), resetSql.getPhaseName(), resetSql.getHost(), resetSql.getPort(), resetSql.getResourceId(), runnerMapVoOptional.get().getUrl(), resetSql.getRunnerId()));
            }
            jobVo.setExecuteJobNodeVoList(jobPhaseNodeVos);
        } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            sqlIdList = sqlIdArray.toJavaList(Long.class);
            jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeListBySqlIdList(sqlIdList));
        }
        return sqlIdList;
    }


    @Override
    public void resetSqlStatus(JSONObject paramObj, AutoexecJobVo jobVo) {
        List<Long> resetSqlIdList = getSqlIdsAndExecuteJobNodes(paramObj, jobVo);
        //批量重置sql文件状态
        if (CollectionUtils.isNotEmpty(resetSqlIdList)) {
            autoexecJobMapper.resetJobSqlStatusBySqlIdList(resetSqlIdList);
        }
    }

    @Override
    public int searchJobPhaseSqlCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        return autoexecJobMapper.searchJobPhaseSqlCount(jobPhaseNodeVo);
    }

    @Override
    public JSONObject searchJobPhaseSql(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<AutoexecSqlNodeDetailVo> returnList = new ArrayList<>();
        int sqlCount = autoexecJobMapper.searchJobPhaseSqlCount(jobPhaseNodeVo);
        if (sqlCount > 0) {
            jobPhaseNodeVo.setRowNum(sqlCount);
            returnList = autoexecJobMapper.searchJobPhaseSql(jobPhaseNodeVo);
        }
        return TableResultUtil.getResult(returnList, jobPhaseNodeVo);
    }

    @Override
    public List<ISqlNodeDetail> searchJobPhaseSqlForExport(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<ISqlNodeDetail> result = new ArrayList<>();
        List<AutoexecSqlNodeDetailVo> list = autoexecJobMapper.searchJobPhaseSql(jobPhaseNodeVo);
        if (list.size() > 0) {
            list.forEach(o -> result.add(o));
        }
        return result;
    }

    @Override
    public void checkinSqlList(JSONObject paramObj) {
        AutoexecJobPhaseVo targetPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(paramObj.getLong("jobId"), paramObj.getString("targetPhaseName"));
        if (targetPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(paramObj.getString("targetPhaseName"));
        }
        JSONArray paramSqlVoArray = paramObj.getJSONArray("sqlInfoList");
        Long updateTag = System.currentTimeMillis();
        if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
            List<AutoexecSqlNodeDetailVo> insertSqlList = paramSqlVoArray.toJavaList(AutoexecSqlNodeDetailVo.class);
            if (insertSqlList.size() > 100) {
                int cyclicNumber = insertSqlList.size() / 100;
                if (insertSqlList.size() % 100 != 0) {
                    cyclicNumber++;
                }
                for (int i = 0; i < cyclicNumber; i++) {
                    autoexecJobMapper.insertSqlDetailList(insertSqlList.subList(i * 100, (Math.min((i + 1) * 100, insertSqlList.size()))), targetPhaseVo.getName(), targetPhaseVo.getId(), paramObj.getLong("runnerId"), updateTag);
                }
            } else {
                autoexecJobMapper.insertSqlDetailList(insertSqlList, targetPhaseVo.getName(), targetPhaseVo.getId(), paramObj.getLong("runnerId"), updateTag);
            }
            List<Long> needDeleteSqlIdList = autoexecJobMapper.getSqlDetailIdListByJobIdAndPhaseNameAndResourceIdAndLcd(paramObj.getLong("jobId"), insertSqlList.get(0).getResourceId(), paramObj.getString("targetPhaseName"), updateTag);
            if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                autoexecJobMapper.updateSqlIsDeleteByIdList(needDeleteSqlIdList);
            }
        }
    }

    @Override
    public void updateSqlStatus(JSONObject paramObj) {
        AutoexecSqlNodeDetailVo paramSqlVo = paramObj.getJSONObject("sqlStatus").toJavaObject(AutoexecSqlNodeDetailVo.class);
        paramSqlVo.setPhaseName(paramObj.getString("phaseName"));
        paramSqlVo.setJobId(paramObj.getLong("jobId"));
        AutoexecSqlNodeDetailVo oldSqlDetailVo = autoexecJobMapper.getJobSqlByResourceIdAndJobIdAndJobPhaseNameAndSqlFile(paramSqlVo.getResourceId(), paramObj.getLong("jobId"), paramObj.getString("phaseName"), paramSqlVo.getSqlFile());
        if (oldSqlDetailVo == null) {
            AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            if (phaseVo == null) {
                throw new AutoexecJobPhaseNotFoundException(paramObj.getString("phaseName"));
            }
            paramSqlVo.setRunnerId(paramObj.getLong("runnerId"));
            paramSqlVo.setJobId(paramObj.getLong("jobId"));
            paramSqlVo.setPhaseId(phaseVo.getId());
            autoexecJobMapper.insertSqlDetail(paramSqlVo);
        } else {
            paramSqlVo.setId(oldSqlDetailVo.getId());
            autoexecJobMapper.updateSqlDetailById(paramSqlVo);
        }
    }

    @Override
    public AutoexecSqlNodeDetailVo getSqlDetail(AutoexecJobVo jobVo) {
        return autoexecJobMapper.getJobSqlByJobPhaseIdAndResourceIdAndSqlName(jobVo.getActionParam().getLong("jobPhaseId"), jobVo.getActionParam().getLong("resourceId"), jobVo.getActionParam().getString("sqlName"));
    }

    @Override
    public List<RunnerMapVo> getRunnerMapList(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        List<RunnerMapVo> runnerMapVos = null;
        if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(jobPhaseVo.getExecMode())) {
            List<GroupNetworkVo> networkVoList = runnerMapper.getAllNetworkMask();
            for (GroupNetworkVo networkVo : networkVoList) {
                if (IpUtil.isBelongSegment(jobPhaseVo.getCurrentNode().getHost(), networkVo.getNetworkIp(), networkVo.getMask())) {
                    RunnerGroupVo groupVo = runnerMapper.getRunnerMapGroupById(networkVo.getGroupId());
                    if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
                        throw new RunnerGroupRunnerNotFoundException(groupVo.getName() + "(" + networkVo.getGroupId() + ") ");
                    }
                    runnerMapVos = groupVo.getRunnerMapList();
                }
            }
        } else {
            runnerMapVos = runnerMapper.getAllRunnerMap();
        }
        return runnerMapVos;
    }

    @Override
    public AutoexecCombopVo getAutoexecCombop(AutoexecJobVo autoexecJobParam) {
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecJobParam.getOperationId());
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecJobParam.getOperationId());
        }
        // 测试组合工具草稿版本时会传入combopVersionId参数，如果combopVersionId为null，则取对应的激活版本数据
        Long versionId = autoexecJobParam.getCombopVersionId();
        if (versionId == null) {
            versionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopVo.getId());
            if (versionId == null) {
                throw new AutoexecCombopActiveVersionNotFoundException(combopVo.getName());
            }
        }
        if (versionId != null) {
            AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
            if (versionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(versionId);
            }
            AutoexecCombopVersionConfigVo versionConfig = versionVo.getConfig();
            if (versionConfig != null) {
                AutoexecCombopConfigVo config = combopVo.getConfig();
                config.setExecuteConfig(versionConfig.getExecuteConfig());
                config.setCombopGroupList(versionConfig.getCombopGroupList());
                config.setCombopPhaseList(versionConfig.getCombopPhaseList());
                config.setRuntimeParamList(versionConfig.getRuntimeParamList());
            }
            if (autoexecJobParam.getInvokeId() == null) {
                autoexecJobParam.setInvokeId(versionId);
            }
        }
        if (StringUtils.isBlank(autoexecJobParam.getName())) {
            autoexecJobParam.setName(combopVo.getName());
        }
        return combopVo;
    }

    @Override
    public AutoexecCombopVo getSnapshotAutoexecCombop(AutoexecJobVo autoexecJobParam) {
        AutoexecCombopVo combopVo = new AutoexecCombopVo();
        combopVo.setConfig(autoexecJobParam.getConfig());
        return combopVo;
    }

    @Override
    public List<AutoexecJobPhaseNodeVo> getJobNodeListBySqlIdList(List<Long> sqlIdList) {
        return autoexecJobMapper.getJobPhaseNodeListBySqlIdList(sqlIdList);
    }

    @Override
    public boolean getIsCanUpdatePhaseRunner(AutoexecJobPhaseVo jobPhaseVo, Long runnerMapId) {
        if (Objects.equals(jobPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            List<AutoexecSqlNodeDetailVo> sqlDetail = autoexecJobMapper.getJobSqlDetailListByJobIdAndPhaseNameAndExceptStatusAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getName(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
            return sqlDetail.size() == 0;
        } else {
            List<AutoexecJobPhaseNodeVo> phaseNodes = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatusAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getId(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
            return phaseNodes.size() == 0;
        }
    }

    @Override
    public void myExecuteAuthCheck(AutoexecJobVo jobVo) {
        //先校验有没有组合工具权限
        if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            if (combopVo == null && jobVo.getIsTakeOver() == 0) {
                throw new AutoexecCombopNotFoundException(jobVo.getOperationId());
            }
            if (combopVo != null && !Objects.equals(combopVo.getOwner(), jobVo.getExecUser()) && !autoexecCombopService.checkOperableButton(combopVo, CombopAuthorityAction.EXECUTE)) {
                throw new AutoexecJobCanNotCreateException(combopVo.getName());
            }
        } else if (Arrays.asList(CombopOperationType.SCRIPT.getValue(), CombopOperationType.TOOL.getValue()).contains(jobVo.getOperationType())) {
            if (!AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                throw new AutoexecScriptJobCanNotExecuteException(jobVo.getId());
            }
        }
    }

    @Override
    public List<String> getModifyAuthList() {
        return Collections.singletonList(AUTOEXEC_JOB_MODIFY.class.getSimpleName());
    }

    @Override
    public void getJobActionAuth(AutoexecJobVo jobVo) {
        if (Objects.equals(jobVo.getSource(), JobSource.TEST.getValue())
                || Objects.equals(jobVo.getSource(), JobSource.SCRIPT_TEST.getValue())
                || Objects.equals(jobVo.getSource(), JobSource.TOOL_TEST.getValue())) {
            if (AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class)) {
                if (UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                    jobVo.setIsCanExecute(1);
                } else {
                    jobVo.setIsCanTakeOver(1);
                }
            }
        } else if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            //如果组合工具已经被删除，则只需要校验执行用户即可
            if (combopVo == null && UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                jobVo.setIsCanExecute(1);
            }
            if (combopVo != null && autoexecCombopService.checkOperableButton(combopVo, CombopAuthorityAction.EXECUTE)) {
                if (UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                    jobVo.setIsCanExecute(1);
                } else {
                    jobVo.setIsCanTakeOver(1);
                }
            }
        }

    }

    @Override
    public void updateSqlStatus(List<Long> sqlIdList, String status) {
        autoexecJobMapper.updateSqlStatusByIdList(sqlIdList, status);
    }

    @Override
    public void getCreatePayload(AutoexecJobVo jobVo, JSONObject result) {
        //executeConfig
        AutoexecJobContentVo configContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        JSONObject jobConfig = JSONObject.parseObject(configContentVo.getContent());
        result.put("executeConfig", jobConfig.getJSONObject("executeConfig"));
        if (Objects.equals(CombopOperationType.COMBOP.getValue(), jobVo.getOperationType())) {
            result.put("combopId", jobVo.getOperationId());
        } else {
            result.put("operationId", jobVo.getOperationId());
            result.put("type", jobVo.getOperationType());
            //argumentMappingList
            JSONArray combopPhaseList = jobConfig.getJSONArray("combopPhaseList");
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                JSONObject phaseConfig = combopPhaseList.getJSONObject(0).getJSONObject("config");
                if (MapUtils.isNotEmpty(phaseConfig)) {
                    JSONArray phaseOperationList = phaseConfig.getJSONArray("phaseOperationList");
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        JSONObject phaseOperation = phaseOperationList.getJSONObject(0);
                        JSONObject operationConfig = phaseOperation.getJSONObject("config");
                        if (MapUtils.isNotEmpty(operationConfig)) {
                            JSONObject operation = phaseOperation.getJSONObject("operation");
                            if (MapUtils.isNotEmpty(operation)) {
                                result.put("execMode", operation.getString("execMode"));
                            }
                            result.put("argumentMappingList", operationConfig.getJSONArray("argumentMappingList"));
                        }
                    }
                }
            }
        }
        result.put("source", jobVo.getSource());
        result.put("name", jobVo.getName());
    }

    @Override
    public void deleteJob(AutoexecJobVo jobVo) {
        autoexecJobMapper.deleteJobSqlDetailByJobId(jobVo.getId());
    }
}
