package neatlogic.module.autoexec.job.source.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
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
import neatlogic.framework.dao.mapper.TagMapper;
import neatlogic.framework.dao.mapper.UserSessionContentMapper;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.TagVo;
import neatlogic.framework.dto.runner.GroupNetworkVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupNotFoundException;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerNotFoundException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.AviatorEvaluatorUtil;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.I18nUtils;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    TagMapper tagMapper;

    @Resource
    UserSessionContentMapper userSessionContentMapper;

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

    @Override
    public void resetSqlStatus(JSONObject paramObj, AutoexecJobVo jobVo) {
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        AutoexecJobPhaseVo currentPhase = jobVo.getExecutePhase();
        if (paramObj.getInteger("isAll") != null && paramObj.getInteger("isAll") == 1) {
            autoexecJobMapper.updateJobSqlStatusByJobIdAndPhaseId(currentPhase.getJobId(), currentPhase.getId(), JobNodeStatus.PENDING.getValue());
        } else {
            List<Long> sqlIdList = sqlIdArray.toJavaList(Long.class);
            //批量重置sql文件状态
            if (CollectionUtils.isNotEmpty(sqlIdList)) {
                jobVo.setJobPhaseNodeSqlList(autoexecJobMapper.getJobPhaseNodeListBySqlIdList(sqlIdList));
                autoexecJobMapper.resetJobSqlStatusBySqlIdList(sqlIdList);
            }
        }
    }

    @Override
    public void ignoreSql(JSONObject paramObj, AutoexecJobVo jobVo) {
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        AutoexecJobPhaseVo currentPhase = jobVo.getExecutePhase();
        if (paramObj.getInteger("isAll") != null && paramObj.getInteger("isAll") == 1) {
            autoexecJobMapper.updateJobSqlStatusByJobIdAndPhaseId(currentPhase.getJobId(), currentPhase.getId(), JobNodeStatus.IGNORED.getValue());
        } else {
            List<Long> sqlIdList = sqlIdArray.toJavaList(Long.class);
            //批量重置sql文件状态
            if (CollectionUtils.isNotEmpty(sqlIdList)) {
                jobVo.setJobPhaseNodeSqlList(autoexecJobMapper.getJobPhaseNodeListBySqlIdList(sqlIdList));
                autoexecJobMapper.updateSqlStatusByIdList(sqlIdList, JobNodeStatus.IGNORED.getValue());
            }
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

    /**
     * 工具库工具sqlfile/sqlcheck会调用此方法
     * <p>
     * 1、按照对应的顺序insert（DUPLICAT）
     * status默认pending
     * isModified默认0
     * wornCount默认为0
     * sort为插入的顺序
     * 2、将需要逻辑删的数据的is_delete改为1，sort改为999999
     *
     * @param paramObj 当前阶段的所有sql信息
     */
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

    /**
     * 工具库工具sqlfile/sqlexec会调用此方法
     * 若sql不存在，则insert
     * 若sql存在则更新
     * status为pending，start_time、end_time不更新
     * status为running，更新start_time为now(3)
     * status为其他状态时，更新end_time为now(3)
     *
     * @param paramObj 单条sql的信息
     */
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

    /**
     * 执行器匹配规则：
     * 1、标签非比填，如果设置了标签且不存在打了该标签的执行器组则抛异常
     * 2、target类型
     * - 如果设置了标签，则必须同时满足标签和网段，否则抛异常
     * - 如果没设置标签，则必须满足网段，否则抛异常
     * 3、runner类型（如果指定执行器组为“随机分配”，则表示任意runner）
     * - 如果设置了标签，则必须同时满足指定执行组和标签，否则抛异常
     * - 如果没设置标签，则必须满足指定执行组，否则抛异常
     * 4、匹配到的执行器组如果没有执行器则抛异常
     *
     * @param jobVo 作业参数
     */
    @Override
    public List<RunnerMapVo> getRunnerMapList(AutoexecJobVo jobVo, AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getExecutePhase();
        List<RunnerMapVo> runnerMapVos = null;
        ParamMappingVo runnerGroupTagParam = jobVo.getRunnerGroupTag();
        String runnerGroupTagStr = null;
        String runnerGroupTagNameStr = StringUtils.EMPTY;
        //满足标签的执行器组id列表
        List<Long> runnerGroupIdListWithTag = new ArrayList<>();
        List<Long> runnerGroupIdListWithTagAndRule;
        List<String> runnerGroupTagList = null;
        Boolean isJobRunnerGroupTag = null;
        List<Long> runnerGroupIdListWithRule = getMatchRuleRunnerGroupList();
        if (CollectionUtils.isEmpty(runnerGroupIdListWithRule)) {
            throw new RunnerGroupMatchRuleNotFoundException();
        }
        //优先获取runner phase声明的执行器组标签
        if (ExecMode.RUNNER.getValue().equals(jobPhaseVo.getExecMode()) && combopPhaseExecuteConfigVo != null && combopPhaseExecuteConfigVo.getExecuteConfig() != null
                && combopPhaseExecuteConfigVo.getExecuteConfig().getRunnerGroupTag() != null) {
            runnerGroupTagStr = autoexecJobService.getFinalParamValue(combopPhaseExecuteConfigVo.getExecuteConfig().getRunnerGroupTag(), jobVo.getRunTimeParamList());
            if (StringUtils.isNotBlank(runnerGroupTagStr) && runnerGroupTagStr.startsWith("[")) {
                runnerGroupTagList = JSON.parseArray(runnerGroupTagStr, String.class);
                if (CollectionUtils.isNotEmpty(runnerGroupTagList)) {
                    isJobRunnerGroupTag = false;
                }
            }
        }
        //其次获取创建作业时声明的执行器组标签
        if (CollectionUtils.isEmpty(runnerGroupTagList) && runnerGroupTagParam != null && runnerGroupTagParam.getValue() != null) {
            runnerGroupTagStr = autoexecJobService.getFinalParamValue(runnerGroupTagParam, jobVo.getRunTimeParamList());
            if (StringUtils.isNotBlank(runnerGroupTagStr) && runnerGroupTagStr.startsWith("[")) {
                runnerGroupTagList = JSON.parseArray(runnerGroupTagStr, String.class);
                if (CollectionUtils.isNotEmpty(runnerGroupTagList)) {
                    isJobRunnerGroupTag = true;
                }
            } else {
                throw new AutoexecRunnerGroupTagInvalidException(runnerGroupTagStr);
            }
        }

        if (CollectionUtils.isNotEmpty(runnerGroupTagList)) {
            List<TagVo> tagVoList = tagMapper.getTagListByIdOrNameList(runnerGroupTagList);
            if (CollectionUtils.isEmpty(tagVoList)) {
                if (Boolean.TRUE.equals(isJobRunnerGroupTag)) {
                    throw new AutoexecRunnerGroupTagInvalidException(runnerGroupTagStr);
                } else {
                    throw new AutoexecRunnerGroupTagInvalidException(runnerGroupTagStr, jobPhaseVo);
                }
            }
            runnerGroupTagNameStr = tagVoList.stream().map(TagVo::getName).collect(Collectors.joining("、"));
            List<RunnerGroupVo> runnerGroupVos = runnerMapper.getRunnerGroupByTagIdOrNameList(runnerGroupTagList);
            if (CollectionUtils.isNotEmpty(runnerGroupVos)) {
                runnerGroupVos = runnerGroupVos.stream().filter(rg -> runnerGroupIdListWithRule.contains(rg.getId())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(runnerGroupVos)) {
                    runnerGroupIdListWithTagAndRule = runnerGroupVos.stream().map(RunnerGroupVo::getId).collect(Collectors.toList());
                } else {
                    if (Boolean.TRUE.equals(isJobRunnerGroupTag)) {
                        throw new AutoexecRunnerGroupNotFoundByTagAndRuleException(runnerGroupTagNameStr);
                    } else {
                        throw new AutoexecRunnerGroupNotFoundByTagAndRuleException(runnerGroupTagNameStr, jobPhaseVo);
                    }
                }
                runnerGroupIdListWithTag = runnerGroupVos.stream().map(RunnerGroupVo::getId).collect(Collectors.toList());
            } else {
                if (Boolean.TRUE.equals(isJobRunnerGroupTag)) {
                    throw new AutoexecRunnerGroupNotFoundByTagException(runnerGroupTagNameStr);
                } else {
                    throw new AutoexecRunnerGroupNotFoundByTagException(runnerGroupTagNameStr, jobPhaseVo);
                }
            }
        } else {
            runnerGroupIdListWithTagAndRule = runnerGroupIdListWithRule;
        }


        if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(jobPhaseVo.getExecMode())) {
            List<GroupNetworkVo> networkVoList = runnerMapper.getAllNetworkMask(runnerGroupIdListWithTagAndRule);
            for (GroupNetworkVo networkVo : networkVoList) {
                if (IpUtil.isBelongSegment(jobPhaseVo.getCurrentNode().getHost(), networkVo.getNetworkIp(), networkVo.getMask())) {
                    RunnerGroupVo groupVo = runnerMapper.getRunnerMapGroupById(networkVo.getGroupId());
                    if (groupVo == null) {
                        throw new RunnerGroupRunnerNotFoundException(networkVo.getGroupId());
                    }
                    if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
                        throw new RunnerGroupRunnerNotFoundException(groupVo.getName(), networkVo.getNetworkIp());
                    }
                    runnerMapVos = groupVo.getRunnerMapList();
                    break;
                }
            }
        } else {
            //默认随机分配
            String runnerGroup = null;
            ParamMappingVo runnerGroupParam = jobVo.getRunnerGroup();
            Boolean isJobRunnerGroup = null;
            //优先使用runner phase声明的执行器组
            if (ExecMode.RUNNER.getValue().equals(jobPhaseVo.getExecMode()) && combopPhaseExecuteConfigVo != null && combopPhaseExecuteConfigVo.getExecuteConfig() != null
                    && combopPhaseExecuteConfigVo.getExecuteConfig().getRunnerGroup() != null) {
                String runnerGroupIdStr = autoexecJobService.getFinalParamValue(combopPhaseExecuteConfigVo.getExecuteConfig().getRunnerGroup(), jobVo.getRunTimeParamList());
                if (StringUtils.isNotBlank(runnerGroupIdStr)) {
                    runnerGroup = runnerGroupIdStr;
                    isJobRunnerGroup = false;
                }
            }

            if (runnerGroup == null && runnerGroupParam != null) {
                String runnerGroupIdStr = autoexecJobService.getFinalParamValue(runnerGroupParam, jobVo.getRunTimeParamList());
                if (StringUtils.isNotBlank(runnerGroupIdStr)) {
                    runnerGroup = runnerGroupIdStr;
                    isJobRunnerGroup = true;
                }
            }

            if (Boolean.TRUE.equals(isJobRunnerGroup) && (isJobRunnerGroupTag == null || Boolean.TRUE.equals(isJobRunnerGroupTag))) {
                autoexecJobMapper.updateJobPhaseRunnerGroupFrom(AutoexecJobPhaseNodeFrom.JOB.getValue(), jobPhaseVo.getId());
                jobPhaseVo.setRunnerGroupFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            } else {
                autoexecJobMapper.updateJobPhaseRunnerGroupFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue(), jobPhaseVo.getId());
                jobPhaseVo.setRunnerGroupFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue());
            }

            //如果都没有设置，则默认为-1，随机分配
            if (runnerGroup == null) {
                runnerGroup = "-1";
            }
            if (Objects.equals(runnerGroup, "-1")) {//-1 代表 “随机匹配”
                runnerMapVos = runnerMapper.getAllRunnerMap(runnerGroupIdListWithTagAndRule);
                if (CollectionUtils.isEmpty(runnerMapVos)) {
                    throw new RunnerNotFoundException();
                }
            } else {
                RunnerGroupVo runnerGroupVo = runnerMapper.getRunnerGroupByIdOrName(runnerGroup);
                if (runnerGroupVo == null) {
                    throw new RunnerGroupNotFoundException(runnerGroup);
                }
                if (CollectionUtils.isNotEmpty(runnerGroupIdListWithTag) && !runnerGroupIdListWithTag.contains(runnerGroupVo.getId())) {
                    String runnerGroupFrom = I18nUtils.getMessage("nmajsa.autoexecjobsourcetypehandler.getrunnermaplist");
                    if (Boolean.FALSE.equals(isJobRunnerGroup)) {
                        runnerGroupFrom = I18nUtils.getMessage("nfac.paramtype.phase") + "“" + jobPhaseVo.getName() + "“";
                    }
                    String runnerGroupTagFrom = I18nUtils.getMessage("nmajsa.autoexecjobsourcetypehandler.getrunnermaplist");
                    if (Boolean.FALSE.equals(isJobRunnerGroupTag)) {
                        runnerGroupTagFrom = I18nUtils.getMessage("nfac.paramtype.phase") + "“" + jobPhaseVo.getName() + "“";
                    }
                    throw new AutoexecRunnerGroupTagNotMatchException(runnerGroupFrom, runnerGroupTagFrom, runnerGroupVo.getName(), runnerGroupTagNameStr);
                }
                if (!runnerGroupIdListWithRule.contains(runnerGroupVo.getId())) {
                    if (Boolean.FALSE.equals(isJobRunnerGroup)) {
                        throw new AutoexecRunnerGroupRuleNotMatchException(runnerGroupVo, I18nUtils.getMessage("nfac.paramtype.phase"), jobPhaseVo.getName());
                    } else {
                        throw new AutoexecRunnerGroupRuleNotMatchException(runnerGroupVo, I18nUtils.getMessage("nmajsa.autoexecjobsourcetypehandler.getrunnermaplist"));
                    }
                }
                runnerMapVos = runnerMapper.getRunnerMapListByRunnerGroupId(runnerGroupVo.getId());
                if (CollectionUtils.isEmpty(runnerMapVos)) {
                    throw new RunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + "(" + runnerGroupVo.getId() + ")");
                }
            }

        }
        return runnerMapVos;
    }

    /**
     * 返回所有rule为空 或 满足rule的执行器组
     */
    private List<Long> getMatchRuleRunnerGroupList() {
        List<Long> matchRuleRunnerGroupIdList = new ArrayList<>();
        List<RunnerGroupVo> runnerGroups = runnerMapper.getAllRunnerGroupList();
        JSONObject headers = null;
        //如果存在规则的执行器组，则先校验
        for (RunnerGroupVo runnerGroupVo : runnerGroups) {
            if (StringUtils.isNotBlank(runnerGroupVo.getRule())) {
                if (MapUtils.isEmpty(headers) && UserContext.get() != null) {
                    String tokenHash = UserContext.get().getTokenHash();
                    if (StringUtils.isNotBlank(tokenHash)) {
                        String sessionContentStr = userSessionContentMapper.getUserSessionContentByHash(tokenHash);
                        JSONObject sessionContentJson = JSON.parseObject(sessionContentStr);
                        if (MapUtils.isNotEmpty(sessionContentJson)) {
                            headers = sessionContentJson.getJSONObject("headers");
                        }
                    }
                }
                if (MapUtils.isNotEmpty(headers) && AviatorEvaluatorUtil.evaluateBoolean(runnerGroupVo.getRule(), headers)) {
                    matchRuleRunnerGroupIdList.add(runnerGroupVo.getId());
                }
            } else {
                matchRuleRunnerGroupIdList.add(runnerGroupVo.getId());
            }
        }
        return matchRuleRunnerGroupIdList;
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
        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
        if (versionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(versionId);
        }
        autoexecCombopService.needExecuteConfig(versionVo);
        AutoexecCombopVersionConfigVo versionConfig = versionVo.getConfig();
        if (versionConfig != null) {
            AutoexecCombopConfigVo config = combopVo.getConfig();
            config.setExecuteConfig(versionConfig.getExecuteConfig());
            config.setCombopGroupList(versionConfig.getCombopGroupList());
            config.setCombopPhaseList(versionConfig.getCombopPhaseList());
            config.setRuntimeParamList(versionConfig.getRuntimeParamList());
            config.setScenarioList(versionConfig.getScenarioList());
        }
        if (autoexecJobParam.getInvokeId() == null) {
            autoexecJobParam.setInvokeId(versionId);
        }
        if (autoexecJobParam.getRouteId() == null) {
            autoexecJobParam.setRouteId(versionId.toString());
        }
        if (StringUtils.isBlank(autoexecJobParam.getName())) {
            autoexecJobParam.setName(combopVo.getName());
        }
        combopVo.setAllPhasesAreRunnerOrSqlExecMode(versionVo.getAllPhasesAreRunnerOrSqlExecMode());
        combopVo.setExistRunnerOrSqlExecMode(versionVo.getExistRunnerOrSqlExecMode());
        combopVo.setNeedExecuteNode(versionVo.getNeedExecuteNode());
        combopVo.setNeedExecuteUser(versionVo.getNeedExecuteUser());
        combopVo.setNeedProtocol(versionVo.getNeedProtocol());
        combopVo.setNeedRoundCount(versionVo.getNeedRoundCount());
        combopVo.setNeedRunnerGroup(versionVo.getNeedRunnerGroup());
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
    public List<String> getPhaseSqlStatusList(AutoexecJobPhaseVo jobPhaseVo, Long
            runnerMapId, List<String> needCountStatusList) {
        return autoexecJobMapper.getJobSqlDetailStatusList(jobPhaseVo.getJobId(), jobPhaseVo.getName(), runnerMapId, needCountStatusList);
    }

    @Override
    public boolean getIsCanUpdateSqlNode(AutoexecJobPhaseVo jobPhaseVo, Long runnerMapId) {
        List<AutoexecSqlNodeDetailVo> sqlDetail = autoexecJobMapper.getJobSqlDetailListByJobIdAndPhaseNameAndExceptStatusAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getName(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
        return sqlDetail.isEmpty();
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
                    if (!Objects.equals(JobStatus.CHECKED.getValue(), jobVo.getStatus())) {
                        jobVo.setIsCanTakeOver(1);
                    }
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
                    if (!Objects.equals(JobStatus.CHECKED.getValue(), jobVo.getStatus())) {
                        jobVo.setIsCanTakeOver(1);
                    }
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
