/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.crossover.IAutoexecJobCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecJobSourceVo;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.autoexec.source.AutoexecJobSourceFactory;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.ConfigMapper;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.dto.ConfigVo;
import codedriver.framework.dto.RestVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerConnectRefusedException;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.exception.runner.RunnerMapNotMatchRunnerException;
import codedriver.framework.exception.runner.RunnerNotMatchException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.RestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * @since 2021/4/12 18:44
 **/
@Service
public class AutoexecJobServiceImpl implements AutoexecJobService, IAutoexecJobCrossoverService {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobServiceImpl.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    AutoexecToolMapper autoexecToolMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Resource
    private AutoexecService autoexecService;
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    RunnerMapper runnerMapper;
    @Resource
    ConfigMapper configMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void saveAutoexecCombopJob(AutoexecJobVo jobVo) {
        AutoexecCombopConfigVo config = jobVo.getConfig();
        if (jobVo.getPlanStartTime().getTime() > System.currentTimeMillis() || Objects.equals(JobTriggerType.MANUAL.getValue(), jobVo.getTriggerType())) {
            jobVo.setStatus(JobStatus.READY.getValue());
        } else if (Objects.equals(JobTriggerType.AUTO.getValue(), jobVo.getTriggerType())) {
            jobVo.setStatus(JobStatus.PENDING.getValue());
        }
        //更新关联来源关系
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        AutoexecJobInvokeVo invokeVo = new AutoexecJobInvokeVo(jobVo.getId(), jobVo.getInvokeId(), jobVo.getSource(), jobSourceVo.getType());
        autoexecJobMapper.insertJobInvoke(invokeVo);
        autoexecJobMapper.insertJob(jobVo);
        autoexecJobMapper.insertIgnoreJobContent(new AutoexecJobContentVo(jobVo.getConfigHash(), jobVo.getConfigStr()));
        autoexecJobMapper.insertIgnoreJobContent(new AutoexecJobContentVo(jobVo.getParamHash(), jobVo.getParamArrayStr()));
        //保存作业执行目标
        AutoexecCombopExecuteConfigVo combopExecuteConfigVo = config.getExecuteConfig();
        String userName = StringUtils.EMPTY;
        Long protocolId = null;
        if (combopExecuteConfigVo != null) {
            //先获取组合工具配置的执行用户和协议
            userName = combopExecuteConfigVo.getExecuteUser();
            protocolId = combopExecuteConfigVo.getProtocolId();
        }
        //获取group Map
        Map<Long, AutoexecJobGroupVo> combopIdJobGroupVoMap = new LinkedHashMap<>();
        for (AutoexecCombopGroupVo combopGroupVo : config.getCombopGroupList()) {
            AutoexecJobGroupVo jobGroupVo = new AutoexecJobGroupVo(combopGroupVo);
            jobGroupVo.setJobId(jobVo.getId());
            combopIdJobGroupVoMap.put(combopGroupVo.getId(), jobGroupVo);
        }
        //保存阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = new ArrayList<>();
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        List<String> scenarioPhaseNameList = null;
        if (jobVo.getScenarioId() != null && CollectionUtils.isNotEmpty(scenarioList)) {
            Optional<AutoexecCombopScenarioVo> scenarioVoOptional = scenarioList.stream().filter(o -> Objects.equals(o.getScenarioId(), jobVo.getScenarioId())).findFirst();
            if (scenarioVoOptional.isPresent()) {
                AutoexecCombopScenarioVo scenarioVo = scenarioVoOptional.get();
                scenarioPhaseNameList = scenarioVo.getCombopPhaseNameList();
            }
        }
        List<Long> combopGroupIdList = new ArrayList<>();//记录真正使用的group
        Map<String, String> preOperationNameMap = new HashMap<>();//记录上游阶段工具uuid对应的名称
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            //如果不是场景定义的phase则无需保存
            if (CollectionUtils.isNotEmpty(scenarioPhaseNameList) && !scenarioPhaseNameList.contains(autoexecCombopPhaseVo.getName())) {
                continue;
            }
            jobVo.setExecuteJobGroupVo(combopIdJobGroupVoMap.get(autoexecCombopPhaseVo.getGroupId()));
            //根据作业来源执行对应保存阶段的动作
            AutoexecJobPhaseVo jobPhaseVo = new AutoexecJobPhaseVo(autoexecCombopPhaseVo, jobVo.getId(), combopIdJobGroupVoMap);
            jobPhaseVo.setIsPreOutputUpdateNode(isNeedUpdateOtherPhaseNodeByOutput(jobVo, jobPhaseVo) ? 1 : 0);
            autoexecJobMapper.insertJobPhase(jobPhaseVo);
            combopGroupIdList.add(autoexecCombopPhaseVo.getGroupId());
            jobPhaseVoList.add(jobPhaseVo);
            AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo = autoexecCombopPhaseVo.getConfig();
            //jobPhaseOperation
            List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = combopPhaseExecuteConfigVo.getPhaseOperationList();
            convertCombOperation2JobOperation(jobPhaseVo, jobPhaseVoList, combopPhaseOperationList, jobVo, preOperationNameMap);
            //jobPhaseNode
            if (isPhaseNodeNeedReInitByPreOutput(jobVo, jobPhaseVo)) {
                //如果需要上游出参作为执行目标则无需初始化执行当前阶段执行目标
                continue;
            }
            //如果是target、runnerTarget、sql 则获取执行目标，否则随机分配runner
            jobVo.setCurrentPhase(jobPhaseVo);
            if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(autoexecCombopPhaseVo.getExecMode())) {
                initPhaseExecuteUserAndProtocolAndNode(jobVo, combopExecuteConfigVo, combopPhaseExecuteConfigVo);
            } else {
                IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
                List<RunnerMapVo> runnerMapList = autoexecJobSourceActionHandler.getRunnerMapList(jobVo);
                if (CollectionUtils.isEmpty(runnerMapList)) {
                    throw new RunnerNotMatchException();
                }
                int runnerMapIndex = (int) (Math.random() * runnerMapList.size());
                RunnerMapVo runnerMapVo = runnerMapList.get(runnerMapIndex);
                Date nowTime = new Date(System.currentTimeMillis());
                jobPhaseVo.setLcd(nowTime);
                AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), jobPhaseVo, "runner", JobNodeStatus.PENDING.getValue(), userName, protocolId);
                autoexecJobMapper.insertJobPhaseNode(nodeVo);
                nodeVo.setRunnerMapId(runnerMapVo.getRunnerMapId());
                autoexecJobMapper.insertIgnoreJobPhaseNodeRunner(new AutoexecJobPhaseNodeRunnerVo(nodeVo));
                autoexecJobMapper.insertJobPhaseRunner(nodeVo.getJobId(), nodeVo.getJobGroupId(), nodeVo.getJobPhaseId(), nodeVo.getRunnerMapId(), nodeVo.getLcd());
                autoexecJobMapper.updateJobPhaseNodeFrom(jobPhaseVo.getId(), AutoexecJobPhaseNodeFrom.PHASE.getValue());
                autoexecJobSourceActionHandler.updateJobRunnerMap(jobVo.getId(), runnerMapVo.getRunnerMapId());
            }
        }
        //保存group
        int i = 0;
        for (Map.Entry<Long, AutoexecJobGroupVo> groupVoEntry : combopIdJobGroupVoMap.entrySet()) {
            if (combopGroupIdList.contains(groupVoEntry.getKey())) {
                groupVoEntry.getValue().setSort(i);
                autoexecJobMapper.insertJobGroup(groupVoEntry.getValue());
                if (i == 0) {
                    jobVo.setExecuteJobGroupVo(groupVoEntry.getValue());
                }
                i++;
            }
        }
    }

    /**
     * 判断阶段是否需要根据"上游出参"更新节点
     *
     * @param jobVo             作业
     * @param currentJobPhaseVo 当前阶段
     * @return true|false
     */
    private boolean isPhaseNodeNeedReInitByPreOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo) {
        AutoexecJobContentVo jobContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        if (jobContentVo == null || StringUtils.isBlank(jobContentVo.getContent())) {
            throw new AutoexecJoConfigNotFoundException(jobVo.getId());
        }
        jobVo.setConfigStr(jobContentVo.getContent());
        AutoexecCombopConfigVo combopConfigVo = jobVo.getConfig();
        if (combopConfigVo != null) {
            List<AutoexecCombopPhaseVo> combopPhaseVos = combopConfigVo.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseVos)) {
                for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVos) {
                    if (!Objects.equals(combopPhaseVo.getName(), currentJobPhaseVo.getName())) {
                        continue;
                    }
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                    if (phaseConfigVo != null) {
                        AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                        if (executeConfigVo != null && executeConfigVo.getIsPresetExecuteConfig() == 1) {
                            AutoexecCombopExecuteNodeConfigVo nodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                            if (nodeConfigVo != null) {
                                if (CollectionUtils.isNotEmpty(nodeConfigVo.getPreOutputList())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断当前阶段是否存在根据出参更新其他阶段执行节点
     *
     * @param jobVo             作业
     * @param currentJobPhaseVo 当前阶段
     * @return true|false
     */
    private boolean isNeedUpdateOtherPhaseNodeByOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo) {
        AutoexecJobContentVo jobContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        if (jobContentVo == null || StringUtils.isBlank(jobContentVo.getContent())) {
            throw new AutoexecJoConfigNotFoundException(jobVo.getId());
        }
        jobVo.setConfigStr(jobContentVo.getContent());
        AutoexecCombopConfigVo combopConfigVo = jobVo.getConfig();
        if (combopConfigVo != null) {
            List<AutoexecCombopPhaseVo> combopPhaseVos = combopConfigVo.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseVos)) {
                for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVos) {
                    if (Objects.equals(combopPhaseVo.getExecMode(), ExecMode.RUNNER.getValue())
                            || combopPhaseVo.getGroupSort() <= currentJobPhaseVo.getJobGroupVo().getSort()
                            || !Objects.equals(currentJobPhaseVo.getJobGroupVo().getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName())
                            || Objects.equals(combopPhaseVo.getName(), currentJobPhaseVo.getName())) {
                        continue;
                    }
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                    if (phaseConfigVo != null) {
                        AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                        if (executeConfigVo != null && executeConfigVo.getIsPresetExecuteConfig() == 1) {
                            AutoexecCombopExecuteNodeConfigVo nodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                            if (nodeConfigVo != null) {
                                if (CollectionUtils.isNotEmpty(nodeConfigVo.getPreOutputList())) {
                                    if (Objects.equals(currentJobPhaseVo.getUuid(), nodeConfigVo.getPreOutputList().get(0))) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 更新根据上游出参更新阶段执行节点
     *
     * @param jobVo             作业
     * @param currentJobPhaseVo 当前作业阶段
     */
    @Override
    public void updateNodeByPreOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo) {
        List<AutoexecCombopPhaseVo> combopPhaseVoList = new ArrayList<>();
        List<AutoexecJobPhaseVo> jobPhaseVoList = getJobPhaseListByPreOutput(jobVo, currentJobPhaseVo, combopPhaseVoList);
        autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(jobPhaseVoList.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()), JobPhaseStatus.PENDING.getValue());
        Map<String, AutoexecJobPhaseVo> jobPhaseUuidMap = jobPhaseVoList.stream().collect(Collectors.toMap(AutoexecJobPhaseVo::getUuid, o -> o));
        for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVoList) {
            jobVo.setCurrentPhase(jobPhaseUuidMap.get(combopPhaseVo.getUuid()));
            initPhaseExecuteUserAndProtocolAndNode(jobVo, jobVo.getConfig().getExecuteConfig(), combopPhaseVo.getConfig());
        }
    }

    @Override
    public List<AutoexecJobPhaseVo> getJobPhaseListByPreOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo) {
        List<AutoexecCombopPhaseVo> combopPhaseVoList = new ArrayList<>();
        return getJobPhaseListByPreOutput(jobVo, currentJobPhaseVo, combopPhaseVoList);
    }

    /**
     * 根据当前阶段出参获取需要更新执行目标的其他阶段
     *
     * @param jobVo             作业
     * @param currentJobPhaseVo 当前阶段
     * @param combopPhaseVoList 需要更新的阶段配置
     * @return 需要更新执行目标的阶段
     */
    private List<AutoexecJobPhaseVo> getJobPhaseListByPreOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo, List<AutoexecCombopPhaseVo> combopPhaseVoList) {
        List<AutoexecJobPhaseVo> jobPhaseVoList = new ArrayList<>();
        AutoexecJobContentVo jobContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        if (jobContentVo == null || StringUtils.isBlank(jobContentVo.getContent())) {
            throw new AutoexecJoConfigNotFoundException(jobVo.getId());
        }
        jobVo.setConfigStr(jobContentVo.getContent());
        AutoexecCombopConfigVo combopConfigVo = jobVo.getConfig();
        if (combopConfigVo != null) {
            List<AutoexecCombopPhaseVo> combopPhaseVos = combopConfigVo.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseVos)) {
                for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVos) {
                    if (Objects.equals(combopPhaseVo.getExecMode(), ExecMode.RUNNER.getValue())
                            || combopPhaseVo.getGroupSort() <= currentJobPhaseVo.getJobGroupVo().getSort()
                            || !Objects.equals(currentJobPhaseVo.getJobGroupVo().getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName())
                            || Objects.equals(combopPhaseVo.getName(), currentJobPhaseVo.getName())) {
                        continue;
                    }
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                    if (phaseConfigVo != null) {
                        AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                        if (executeConfigVo != null && executeConfigVo.getIsPresetExecuteConfig() == 1) {
                            AutoexecCombopExecuteNodeConfigVo nodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                            if (nodeConfigVo != null) {
                                if (CollectionUtils.isNotEmpty(nodeConfigVo.getPreOutputList())) {
                                    if (Objects.equals(currentJobPhaseVo.getUuid(), nodeConfigVo.getPreOutputList().get(0))) {
                                        combopPhaseVoList.add(combopPhaseVo);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseUuidList(jobVo.getId(), combopPhaseVoList.stream().map(AutoexecCombopPhaseVo::getUuid).collect(Collectors.toList()));
        }
        return jobPhaseVoList;
    }

    /**
     * 将组合工具的工具转为作业工具
     *
     * @param jobPhaseVo               当前作业阶段
     * @param jobPhaseVoList           作业所有阶段
     * @param combopPhaseOperationList 组合工具阶段工具列表
     * @param jobVo                    作业
     * @param preOperationNameMap      记录上游阶段工具uuid对应的名称
     * @return 作业工具列表
     */
    private List<AutoexecJobPhaseOperationVo> convertCombOperation2JobOperation(AutoexecJobPhaseVo jobPhaseVo, List<AutoexecJobPhaseVo> jobPhaseVoList, List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList, AutoexecJobVo jobVo, Map<String, String> preOperationNameMap) {
        List<AutoexecJobPhaseOperationVo> jobPhaseOperationVoList = new ArrayList<>();
        jobPhaseVo.setOperationList(jobPhaseOperationVoList);
        for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : combopPhaseOperationList) {
            preOperationNameMap.put(autoexecCombopPhaseOperationVo.getUuid(), autoexecCombopPhaseOperationVo.getOperationName());
            String operationType = autoexecCombopPhaseOperationVo.getOperationType();
            Long id = autoexecCombopPhaseOperationVo.getOperationId();
            //测试 指定脚本id
            autoexecService.getAutoexecOperationBaseVoByIdAndType(jobPhaseVo.getName(), autoexecCombopPhaseOperationVo, true);
            AutoexecJobPhaseOperationVo jobPhaseOperationVo = null;
            if (CombopOperationType.SCRIPT.getValue().equalsIgnoreCase(operationType)) {
                AutoexecScriptVo scriptVo;
                AutoexecScriptVersionVo scriptVersionVo;
                String script;
                if (Objects.equals(jobVo.getSource(), JobSource.TEST.getValue())) {
                    scriptVersionVo = autoexecScriptMapper.getVersionByVersionId(autoexecCombopPhaseOperationVo.getScriptVersionId());
                    if (scriptVersionVo == null) {
                        throw new AutoexecScriptVersionNotFoundException(id);
                    }
                    scriptVo = autoexecScriptMapper.getScriptBaseInfoById(scriptVersionVo.getScriptId());
                    script = autoexecCombopService.getScriptVersionContent(scriptVersionVo);
                } else {
                    scriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
                    scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(id);
                    script = autoexecCombopService.getOperationActiveVersionScriptByOperationId(id);
                }
                jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, scriptVo, scriptVersionVo, script, jobPhaseVoList, preOperationNameMap);
                initIfBlockOperation(autoexecCombopPhaseOperationVo, jobPhaseOperationVo, jobPhaseVo, jobPhaseVoList, jobVo, preOperationNameMap);
            } else {
                AutoexecToolVo toolVo = autoexecToolMapper.getToolById(id);
                jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, toolVo, jobPhaseVoList, preOperationNameMap);
                initIfBlockOperation(autoexecCombopPhaseOperationVo, jobPhaseOperationVo, jobPhaseVo, jobPhaseVoList, jobVo, preOperationNameMap);
            }
            jobPhaseOperationVoList.add(jobPhaseOperationVo);
            autoexecJobMapper.insertJobPhaseOperation(jobPhaseOperationVo);
            autoexecJobMapper.insertIgnoreJobContent(new AutoexecJobContentVo(jobPhaseOperationVo.getParamHash(), jobPhaseOperationVo.getParamStr()));
        }
        return jobPhaseOperationVoList;
    }

    /**
     * @param autoexecCombopPhaseOperationVo 组合工具阶段工具列表
     * @param jobPhaseOperationVo            作业工具
     * @param jobPhaseVo                     当前作业阶段
     * @param jobPhaseVoList                 作业所有阶段列表
     * @param jobVo                          作业
     * @param preOperationNameMap            记录上游阶段工具uuid对应的名称
     */
    private void initIfBlockOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo, AutoexecJobPhaseOperationVo jobPhaseOperationVo, AutoexecJobPhaseVo jobPhaseVo, List<AutoexecJobPhaseVo> jobPhaseVoList, AutoexecJobVo jobVo, Map<String, String> preOperationNameMap) {
        AutoexecCombopPhaseOperationConfigVo combopPhaseOperationConfigVo = autoexecCombopPhaseOperationVo.getConfig();
        if (combopPhaseOperationConfigVo != null) {
            String ifBlockCondition = combopPhaseOperationConfigVo.getCondition();
            if (StringUtils.isNotBlank(ifBlockCondition)) {
                JSONObject paramObj = jobPhaseOperationVo.getParam();
                paramObj.put("condition", combopPhaseOperationConfigVo.getCondition());
                if (CollectionUtils.isNotEmpty(combopPhaseOperationConfigVo.getIfList())) {
                    List<AutoexecCombopPhaseOperationVo> ifOperationList = combopPhaseOperationConfigVo.getIfList();
                    ifOperationList.forEach(o -> {
                        o.setParentOperationId(jobPhaseOperationVo.getId());
                        o.setParentOperationType("if");
                    });
                    List<AutoexecJobPhaseOperationVo> ifJobOperation = convertCombOperation2JobOperation(jobPhaseVo, jobPhaseVoList, ifOperationList, jobVo, preOperationNameMap);

                    paramObj.put("ifList", ifJobOperation);
                }
                if (CollectionUtils.isNotEmpty(combopPhaseOperationConfigVo.getElseList())) {
                    List<AutoexecCombopPhaseOperationVo> elseOperationList = combopPhaseOperationConfigVo.getElseList();
                    elseOperationList.forEach(o -> {
                        o.setParentOperationId(jobPhaseOperationVo.getId());
                        o.setParentOperationType("else");
                    });
                    List<AutoexecJobPhaseOperationVo> elseJobOperation = convertCombOperation2JobOperation(jobPhaseVo, jobPhaseVoList, elseOperationList, jobVo, preOperationNameMap);
                    paramObj.put("elseList", elseJobOperation);
                }
                jobPhaseOperationVo.setParamStr(paramObj.toString());
            }
        }
    }

    @Override
    public void initPhaseExecuteUserAndProtocolAndNode(AutoexecJobVo jobVo, AutoexecCombopExecuteConfigVo combopExecuteConfigVo, AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo) {
        boolean isHasNode = false;
        boolean isPhaseConfig = false;
        boolean isGroupConfig = false;
        String userName = null;
        Long protocolId = null;
        if (combopExecuteConfigVo != null) {
            //先获取组合工具配置的执行用户和协议
            userName = combopExecuteConfigVo.getExecuteUser();
            protocolId = combopExecuteConfigVo.getProtocolId();
        }
        AutoexecCombopExecuteConfigVo executeConfigVo;
        AutoexecJobGroupVo jobGroupVo = jobVo.getCurrentPhase().getJobGroupVo();
        //判断group是不是grayScale，如果是则从group中获取执行节点
        if (Objects.equals(jobGroupVo.getPolicy(), AutoexecJobGroupPolicy.GRAYSCALE.getName())) {
            AutoexecCombopGroupConfigVo groupConfig = jobGroupVo.getConfig();
            if (groupConfig != null) {
                executeConfigVo = groupConfig.getExecuteConfig();
                //判断组执行节点是否配置
                if (executeConfigVo != null) {
                    isGroupConfig = executeConfigVo.getExecuteNodeConfig() != null && !executeConfigVo.getExecuteNodeConfig().isNull();
                    if (isGroupConfig) {
                        jobVo.setNodeFrom(AutoexecJobPhaseNodeFrom.GROUP.getValue());
                        isHasNode = getJobNodeList(executeConfigVo, jobVo, userName, protocolId);
                    }
                }
            }
        } else {
            executeConfigVo = combopPhaseExecuteConfigVo.getExecuteConfig();
            if (executeConfigVo != null && executeConfigVo.getIsPresetExecuteConfig() == 1) {
                if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                    userName = executeConfigVo.getExecuteUser();
                }
                if (executeConfigVo.getProtocolId() != null) {
                    protocolId = executeConfigVo.getProtocolId();
                }

                //判断阶段执行节点是否配置
                isPhaseConfig = executeConfigVo.getExecuteNodeConfig() != null && !executeConfigVo.getExecuteNodeConfig().isNull();
                if (isPhaseConfig) {
                    jobVo.setNodeFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue());
                    isHasNode = getJobNodeList(executeConfigVo, jobVo, userName, protocolId);
                }
            }
        }
        //如果阶段没有设置执行目标，则使用全局执行目标
        if (!isPhaseConfig && !isGroupConfig) {
            jobVo.setNodeFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            isHasNode = getJobNodeList(combopExecuteConfigVo, jobVo, userName, protocolId);
        }
        //如果都找不到执行节点
        if (!isHasNode) {
            throw new AutoexecJobPhaseNodeNotFoundException(jobVo.getCurrentPhase().getName(), isPhaseConfig);
        }

        //跟新节点来源
        autoexecJobMapper.updateJobPhaseNodeFrom(jobVo.getCurrentPhase().getId(), jobVo.getNodeFrom());

    }

    @Override
    public void refreshJobParam(Long jobId, JSONObject paramJson) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (!Objects.equals(CombopOperationType.COMBOP.getValue(), jobVo.getOperationType())) {
            throw new AutoexecJobPhaseOperationMustBeCombopException();
        }
        //补充运行参数真实的值
        List<AutoexecParamVo> paramList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(jobVo.getOperationId());
        JSONArray combopParamsResult = new JSONArray();
        if (MapUtils.isNotEmpty(paramJson)) {
            JSONArray combopParams = JSONArray.parseArray(JSONArray.toJSONString(paramList));
            for (Object combopParam : combopParams) {
                JSONObject combopParamJson = JSONObject.parseObject(combopParam.toString());
                if (MapUtils.isNotEmpty(combopParamJson)) {
                    String key = combopParamJson.getString("key");
                    if (StringUtils.isNotBlank(key)) {
                        Object value = paramJson.get(key);
                        combopParamJson.put("value", value);
                        combopParamsResult.add(combopParamJson);
                    }
                }
            }
        }
        jobVo.setParamArrayStr(combopParamsResult.toJSONString());
        autoexecJobMapper.insertIgnoreJobContent(new AutoexecJobContentVo(jobVo.getParamHash(), jobVo.getParamArrayStr()));
        autoexecJobMapper.updateJobParamHashById(jobVo.getId(), jobVo.getParamHash());
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, List<AutoexecJobPhaseVo> jobPhaseVoList) {
        refreshJobPhaseNodeList(jobId, jobPhaseVoList, null);
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, List<AutoexecJobPhaseVo> jobPhaseVoList, JSONObject executeConfig) {
        AutoexecCombopExecuteConfigVo combopExecuteConfigVo = null;
        //优先使用传进来的执行节点
        if (MapUtils.isNotEmpty(executeConfig)) {
            combopExecuteConfigVo = JSON.toJavaObject(executeConfig, AutoexecCombopExecuteConfigVo.class);
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        jobVo.setConfigStr(autoexecJobMapper.getJobContent(jobVo.getConfigHash()).getContent());
        getAutoexecJobDetail(jobVo);
        AutoexecCombopConfigVo configVo = jobVo.getConfig();
        //获取组合工具执行目标 执行用户和协议
        //非空场景，用于重跑替换执行配置（执行目标，用户，协议）
        if (combopExecuteConfigVo == null) {
            combopExecuteConfigVo = configVo.getExecuteConfig();
        }
        //只刷新当前target|sql阶段
        List<AutoexecCombopPhaseVo> combopPhaseList = configVo.getCombopPhaseList().stream().filter(o -> Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(o.getExecMode())).collect(Collectors.toList());
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo = autoexecCombopPhaseVo.getConfig();
            Optional<AutoexecJobPhaseVo> jobPhaseVoOptional = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getName(), autoexecCombopPhaseVo.getName())).findFirst();
            if (jobPhaseVoOptional.isPresent()) {
                AutoexecJobPhaseVo jobPhaseVo = jobPhaseVoOptional.get();
                jobPhaseVo.setCombopId(jobVo.getOperationId());
                jobVo.setCurrentPhase(jobPhaseVo);
                initPhaseExecuteUserAndProtocolAndNode(jobVo, combopExecuteConfigVo, combopPhaseExecuteConfigVo);
            }
        }
    }

    @Override
    public void refreshJobNodeList(Long jobId) {
        refreshJobNodeList(jobId, null);
    }

    @Override
    public void refreshJobNodeList(Long jobId, JSONObject executeConfig) {
        List<AutoexecJobPhaseVo> phaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobId);
        refreshJobPhaseNodeList(jobId, phaseVoList, executeConfig);
    }

    @Override
    public void getAutoexecJobDetail(AutoexecJobVo jobVo) {
        AutoexecJobContentVo paramContentVo = autoexecJobMapper.getJobContent(jobVo.getParamHash());
        if (paramContentVo != null) {
            jobVo.setParamArrayStr(paramContentVo.getContent());
        }
        AutoexecJobContentVo jobContent = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        jobVo.setConfigStr(jobContent.getContent());
        List<AutoexecJobPhaseVo> jobPhaseVoList = jobVo.getPhaseList();
        AutoexecJobGroupVo executeJobGroupVo = jobVo.getExecuteJobGroupVo();
        if (executeJobGroupVo != null) {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobVo.getId(), executeJobGroupVo.getSort());
        }
        List<AutoexecJobGroupVo> jobGroupVos = autoexecJobMapper.getJobGroupByJobId(jobVo.getId());
        Map<Long, AutoexecJobGroupVo> jobGroupIdMap = jobGroupVos.stream().collect(Collectors.toMap(AutoexecJobGroupVo::getId, e -> e));
        if (CollectionUtils.isNotEmpty(jobPhaseVoList)) {
            AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
            if (jobSourceVo == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
            AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getSnapshotAutoexecCombop(jobVo);
            Map<String, String> descriptionMap = new HashMap<>();
            for (AutoexecCombopPhaseVo combopPhase : combopVo.getConfig().getCombopPhaseList()) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhase.getConfig();
                if (phaseConfigVo != null) {
                    List<AutoexecCombopPhaseOperationVo> operationVoList = phaseConfigVo.getPhaseOperationList();
                    descriptionMap.putAll(operationVoList.stream().collect(Collectors.toMap(AutoexecCombopPhaseOperationVo::getUuid, o -> o.getDescription() == null ? StringUtils.EMPTY : o.getDescription())));
                }
            }
            for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
                phaseVo.setJobGroupVo(jobGroupIdMap.get(phaseVo.getGroupId()));
                List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobVo.getId(), phaseVo.getId());
                phaseVo.setOperationList(operationVoList);
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    paramContentVo = autoexecJobMapper.getJobContent(operationVo.getParamHash());
                    if (paramContentVo != null) {
                        operationVo.setParamStr(paramContentVo.getContent());
                    }
                    operationVo.setDescription(descriptionMap.get(operationVo.getUuid()));
                }
            }
        }
    }


    /**
     * 根据目标ip自动匹配runner
     *
     * @param jobVo 作业参数
     * @return runnerId
     */
    private Long getRunnerByTargetIp(AutoexecJobVo jobVo) {
        if (jobVo.getCurrentPhase().getCurrentNode() != null) {
            AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
            if (jobSourceVo == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
            List<RunnerMapVo> runnerMapVos = autoexecJobSourceActionHandler.getRunnerMapList(jobVo);
            if (CollectionUtils.isNotEmpty(runnerMapVos)) {
                int runnerMapIndex = (int) (jobVo.getCurrentPhase().getCurrentNode().getId() % runnerMapVos.size());
                RunnerMapVo runnerMapVo = runnerMapVos.get(runnerMapIndex);
                if (runnerMapVo.getRunnerMapId() == null) {
                    runnerMapVo.setRunnerMapId(runnerMapVo.getId());
                    runnerMapper.insertRunnerMap(runnerMapVo);
                }
                return runnerMapVo.getRunnerMapId();
            }
        }
        return null;
    }

    /**
     * @param combopExecuteConfigVo node配置config
     * @param jobVo                 作业Vo
     * @param userName              连接node 用户
     * @param protocolId            连接node 协议Id
     */
    private boolean getJobNodeList(AutoexecCombopExecuteConfigVo combopExecuteConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        if (combopExecuteConfigVo == null) {
            return false;
        }
        boolean isHasNode = false;
        Date nowTime = new Date(System.currentTimeMillis());
        jobVo.getCurrentPhase().setLcd(nowTime);

        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = combopExecuteConfigVo.getExecuteNodeConfig();
        if (executeNodeConfigVo == null) {
            return false;
        }
        if (MapUtils.isNotEmpty(executeNodeConfigVo.getFilter())) {
            isHasNode = updateNodeResourceByFilter(executeNodeConfigVo, jobVo, userName, protocolId);
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList())) {
            isHasNode = updateNodeResourceByInput(executeNodeConfigVo, jobVo, userName, protocolId);
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            isHasNode = updateNodeResourceBySelect(executeNodeConfigVo, jobVo, userName, protocolId);
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getParamList())) {
            isHasNode = updateNodeResourceByParam(jobVo, executeNodeConfigVo, userName, protocolId);
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getPreOutputList())) {
            isHasNode = updateNodeResourceByPrePhaseOutput(jobVo, executeNodeConfigVo, userName, protocolId);
        }

        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        boolean isNeedLncd;//用于判断是否需要更新lncd（用于判断是否需要重新下载节点）
        //删除没有跑过的历史节点 runnerMap
        autoexecJobMapper.deleteJobPhaseNodeRunnerByJobPhaseIdAndLcdAndStatus(jobPhaseVo.getId(), nowTime, JobNodeStatus.PENDING.getValue());
        //删除没有跑过的历史节点
        Integer deleteCount = autoexecJobMapper.deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus(jobPhaseVo.getId(), nowTime, JobNodeStatus.PENDING.getValue());
        isNeedLncd = deleteCount > 0;
        //更新该阶段所有不是最近更新的节点为已删除，即非法历史节点
        Integer updateCount = autoexecJobMapper.updateJobPhaseNodeIsDeleteByJobPhaseIdAndLcd(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        isNeedLncd = isNeedLncd || updateCount > 0;
        //阶段节点被真删除||伪删除（is_delete=1），则更新上一次修改日期(plcd),需重新下载
        if (isNeedLncd) {
            if (Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), jobVo.getNodeFrom())) {
                autoexecJobMapper.updateJobLncdById(jobVo.getId(), nowTime);
            } else if (Objects.equals(AutoexecJobPhaseNodeFrom.GROUP.getValue(), jobVo.getNodeFrom())) {
                autoexecJobMapper.updateJobGroupLncdById(jobVo.getExecuteJobGroupVo().getId(), nowTime);
            } else {
                autoexecJobMapper.updateJobPhaseLncdById(jobPhaseVo.getId(), nowTime);
            }
        }
        //更新最近一次修改时间lcd
        autoexecJobMapper.updateJobPhaseLcdById(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        //更新phase runner
        List<RunnerMapVo> jobPhaseNodeRunnerList = autoexecJobMapper.getJobPhaseNodeRunnerListByJobPhaseId(jobPhaseVo.getId());
        List<RunnerMapVo> originPhaseRunnerVoList = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), Collections.singletonList(jobPhaseVo.getId()));
        List<RunnerMapVo> deleteRunnerList = originPhaseRunnerVoList.stream().filter(o -> jobPhaseNodeRunnerList.stream().noneMatch(j -> Objects.equals(o.getRunnerMapId(), j.getRunnerMapId()))).collect(Collectors.toList());
        for (RunnerMapVo deleteRunnerVo : deleteRunnerList) {
            autoexecJobMapper.deleteJobPhaseRunnerByJobPhaseIdAndRunnerMapId(jobPhaseVo.getId(), deleteRunnerVo.getRunnerMapId());
        }
        List<RunnerMapVo> insertRunnerList = jobPhaseNodeRunnerList.stream().filter(j -> originPhaseRunnerVoList.stream().noneMatch(o -> Objects.equals(o.getRunnerMapId(), j.getRunnerMapId()))).collect(Collectors.toList());
        for (RunnerMapVo insertRunnerVo : insertRunnerList) {
            autoexecJobMapper.insertJobPhaseRunner(jobVo.getId(), jobPhaseVo.getGroupId(), jobPhaseVo.getId(), insertRunnerVo.getRunnerMapId(), jobPhaseVo.getLcd());
        }
        return isHasNode;
    }

    /**
     * param
     * 根据运行参数中定义的节点参数 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceByParam(AutoexecJobVo jobVo, AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, String userName, Long protocolId) {
        List<String> paramList = executeNodeConfigVo.getParamList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            JSONArray paramArray = jobVo.getParamArray();
            Set<Long> resourceIdSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(paramArray)) {
                List<Object> paramObjList = paramArray.stream().filter(p -> paramList.contains(((JSONObject) p).getString("key"))).collect(Collectors.toList());
                paramObjList.forEach(p -> {
                    JSONArray valueArray = ((JSONObject) p).getJSONArray("value");
                    for (int i = 0; i < valueArray.size(); i++) {
                        resourceIdSet.add(valueArray.getJSONObject(i).getLong("id"));
                    }
                });
                if (CollectionUtils.isNotEmpty(resourceIdSet)) {
                    IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
                    List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceByIdList(new ArrayList<>(resourceIdSet));
                    if (CollectionUtils.isNotEmpty(resourceVoList)) {
                        updateJobPhaseNode(jobVo, resourceVoList, userName, protocolId);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * param
     * 根据上游阶段出参 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceByPrePhaseOutput(AutoexecJobVo jobVo, AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, String userName, Long protocolId) {
        List<String> preOutputList = executeNodeConfigVo.getPreOutputList();
        if (CollectionUtils.isEmpty(preOutputList) && preOutputList.size() != 3) {
            throw new AutoexecJobUpdateNodeByPreOutPutListException(jobVo);
        }
        String phaseUuid = preOutputList.get(0);
        String operationUuid = preOutputList.get(1);
        String paramKey = preOutputList.get(2);
        AutoexecJobPhaseOperationVo operationVo = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseUuidAndUuid(jobVo.getId(), phaseUuid, operationUuid);
        if (operationVo == null) {
            throw new AutoexecJobPhaseOperationNotFoundException(operationUuid);
        }

        //从mongodb获取output 对应应用的param 值 作为执行节点
        AtomicReference<JSONArray> nodeArrayAtomic = new AtomicReference<>();
        Document doc = new Document();
        Document fieldDocument = new Document();
        doc.put("jobId", jobVo.getId().toString());
        fieldDocument.put("data", true);
        mongoTemplate.getDb().getCollection("_node_output").find(doc).projection(fieldDocument).forEach(o -> {
            JSONObject operation = JSONObject.parseObject(o.toJson());
            if (operation.containsKey("data")) {
                JSONObject dataJson = operation.getJSONObject("data");
                JSONObject outputJson = dataJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
                if (MapUtils.isNotEmpty(outputJson)) {
                    Object nodes = outputJson.get(paramKey);
                    if (nodes instanceof JSONArray) {
                        nodeArrayAtomic.set((JSONArray) nodes);
                    }
                    if (nodes instanceof String) {
                        try {
                            nodeArrayAtomic.set(JSONArray.parseArray(nodes.toString()));
                        } catch (Exception ex) {
                            throw new AutoexecJobNodePreParamValueNotInvalidException(jobVo.getId(), jobVo.getCurrentPhase().getName());
                        }
                    } else {
                        throw new AutoexecJobNodePreParamValueNotInvalidException(jobVo.getId(), jobVo.getCurrentPhase().getName());
                    }
                }
            }
        });
        JSONArray nodeArray = nodeArrayAtomic.get();
        if (CollectionUtils.isEmpty(nodeArray)) {
            throw new AutoexecJobNodePreParamValueNotInvalidException(jobVo.getId(), jobVo.getCurrentPhase().getName());
        }

        //更新执行节点
        List<AutoexecNodeVo> nodeVoList = nodeArray.toJavaList(AutoexecNodeVo.class);
        List<ResourceVo> ipPortNameList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            nodeVoList.forEach(o -> {
                ipPortNameList.add(new ResourceVo(o.getIp(), o.getPort(), o.getName()));
            });
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceListByResourceVoList(ipPortNameList);
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                updateJobPhaseNode(jobVo, resourceVoList, userName, protocolId);
                //重置节点状态
                List<AutoexecJobPhaseNodeVo> jobNodeVoList = autoexecJobMapper.getJobPhaseNodeListWithRunnerByJobPhaseIdAndExceptStatusList(jobVo.getCurrentPhase().getId(), Collections.singletonList(JobNodeStatus.IGNORED.getValue()));
                resetJobNodeStatus(jobVo, jobNodeVoList);
                return true;
            }
        }
        return false;
    }

    /**
     * inputNodeList、selectNodeList
     * 根据输入和选择节点 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceByInput(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = executeNodeConfigVo.getInputNodeList();
        List<ResourceVo> ipPortNameList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            nodeVoList.forEach(o -> {
                ipPortNameList.add(new ResourceVo(o.getIp(), o.getPort(), o.getName()));
            });
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceListByResourceVoList(ipPortNameList);
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                updateJobPhaseNode(jobVo, resourceVoList, userName, protocolId);
                return true;
            }
        }
        return false;
    }

    /**
     * selectNodeList
     * 根据输入和选择节点 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceBySelect(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = executeNodeConfigVo.getSelectNodeList();
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceByIdList(nodeVoList.stream().map(AutoexecNodeVo::getId).collect(toList()));
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                updateJobPhaseNode(jobVo, resourceVoList, userName, protocolId);
                return true;
            }
        }
        return false;
    }

    /**
     * filter
     * 根据过滤器 更新节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceByFilter(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        JSONObject filterJson = executeNodeConfigVo.getFilter();
        if (MapUtils.isNotEmpty(filterJson)) {
            JSONObject resourceJson;
            IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            filterJson.put("pageSize", 100);
            ResourceSearchVo searchVo = resourceCrossoverService.assembleResourceSearchVo(filterJson);
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            int count = resourceCrossoverMapper.getResourceCount(searchVo);
            if (count > 0) {
                int pageCount = PageUtil.getPageCount(count, searchVo.getPageSize());
                for (int i = 1; i <= pageCount; i++) {
                    searchVo.setCurrentPage(i);
                    List<Long> idList = resourceCrossoverMapper.getResourceIdList(searchVo);
                    if (CollectionUtils.isEmpty(idList)) {
                        continue;
                    }
                    List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceListByIdList(idList);
                    if (CollectionUtils.isNotEmpty(resourceVoList)) {
                        updateJobPhaseNode(jobVo, resourceVoList, userName, protocolId);
                    }
                }
                return true;
            }

        }
        return false;
    }

    private void updateJobPhaseNode(AutoexecJobVo jobVo, List<ResourceVo> resourceVoList, String userName, Long protocolId) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        boolean isNeedLncd;//用于判断是否需要更新lncd（用于判断是否需要重新下载节点）
        //新增节点需重新下载
        List<AutoexecJobPhaseNodeVo> originNodeList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdList(jobPhaseVo.getId(), resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList()));
        isNeedLncd = originNodeList.size() != resourceVoList.size();
        //恢复删除节点需重新下载
        if (!isNeedLncd) {
            List<AutoexecJobPhaseNodeVo> originDeleteNodeList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdListAndIsDelete(jobPhaseVo.getId(), resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList()));
            isNeedLncd = originDeleteNodeList.size() > 0;
        }
        if (isNeedLncd) {
            //重新下载
            autoexecJobMapper.updateJobPhaseLncdById(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        }
        resourceVoList.forEach(resourceVo -> {
            AutoexecJobPhaseNodeVo jobPhaseNodeVo = new AutoexecJobPhaseNodeVo(resourceVo, jobPhaseVo.getJobId(), jobPhaseVo, JobNodeStatus.PENDING.getValue(), userName, protocolId);
            jobPhaseVo.setCurrentNode(jobPhaseNodeVo);
            jobPhaseNodeVo.setPort(resourceVo.getPort());
            jobPhaseNodeVo.setRunnerMapId(getRunnerByTargetIp(jobVo));
            if (jobPhaseNodeVo.getRunnerMapId() == null) {
                throw new RunnerNotMatchException(jobPhaseNodeVo.getHost());
            }
            //如果大于 update 0,说明存在旧数据
            Integer result = autoexecJobMapper.updateJobPhaseNodeByJobIdAndPhaseIdAndResourceId(jobPhaseNodeVo);
            if (result == null || result == 0) {
                autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
                //防止旧resource 所以ignore insert
                autoexecJobMapper.insertIgnoreJobPhaseNodeRunner(new AutoexecJobPhaseNodeRunnerVo(jobPhaseNodeVo));
            }
        });
    }

    @Override
    public boolean checkIsAllActivePhaseIsCompleted(Long jobId, Integer groupSort) {
        boolean isDone = false;
        Integer phaseNotCompletedCount = autoexecJobMapper.getJobPhaseNotCompletedCountByJobIdAndGroupSort(jobId, groupSort);
        Integer phaseRunnerNotCompletedCount = autoexecJobMapper.getJobPhaseRunnerNotCompletedCountByJobIdAndIsFireNextAndGroupSort(jobId, 0, groupSort);
        if (phaseNotCompletedCount == 0 && phaseRunnerNotCompletedCount == 0) {
            isDone = true;
        }
        return isDone;
    }

    @Override
    public void setIsRefresh(List<AutoexecJobPhaseVo> jobPhaseVoList, JSONObject paramObj, AutoexecJobVo jobVo, String jobStatusOld) {
        paramObj.put("isRefresh", 1);
        if (Objects.equals(JobStatus.READY.getValue(), jobStatusOld) ||
                (
                        (Objects.equals(JobStatus.COMPLETED.getValue(), jobStatusOld) && Objects.equals(JobStatus.COMPLETED.getValue(), jobVo.getStatus()))
                                || (Objects.equals(JobStatus.ABORTED.getValue(), jobStatusOld) && Objects.equals(JobStatus.ABORTED.getValue(), jobVo.getStatus()))
                                || (Objects.equals(JobStatus.FAILED.getValue(), jobStatusOld) && Objects.equals(JobStatus.FAILED.getValue(), jobVo.getStatus()))
                ) && jobPhaseVoList.stream().noneMatch(o -> Objects.equals(JobPhaseStatus.RUNNING.getValue(), o.getStatus()))
        ) {
            paramObj.put("isRefresh", 0);
        }
    }

    @Override
    public void deleteJob(Long jobId) {
        //删除jobParamContent
        /*Set<String> hashSet = new HashSet<>();
        hashSet.add(jobVo.getParamHash());
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobId(jobId);
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            hashSet.add(operationVo.getParamHash());
        }
        for (String hash : hashSet) {
            AutoexecJobParamContentVo paramContentVo = autoexecJobMapper.getJobParamContentLock(hash);
            if(paramContentVo != null) {
                int jobParamReferenceCount = autoexecJobMapper.checkIsJobParamReference(jobId, hash);
                int jobPhaseOperationParamReferenceCount = autoexecJobMapper.checkIsJobPhaseOperationParamReference(jobId, hash);
                if (jobParamReferenceCount == 0 && jobPhaseOperationParamReferenceCount == 0) {
                    autoexecJobMapper.deleteJobParamContentByHash(hash);
                }
            }
        }*/
        //else
        autoexecJobMapper.deleteJobEvnByJobId(jobId);
        autoexecJobMapper.deleteJobInvokeByJobId(jobId);
        autoexecJobMapper.deleteJobResourceInspectByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseOperationByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseByJobId(jobId);
        autoexecJobMapper.deleteJobByJobId(jobId);
    }

    @Override
    public List<AutoexecJobVo> searchJob(AutoexecJobVo jobVo) {
        List<AutoexecJobVo> jobVoList = new ArrayList<>();
        List<Long> jobIdList = jobVo.getIdList();
        if (CollectionUtils.isEmpty(jobIdList)) {
            int rowNum = autoexecJobMapper.searchJobCount(jobVo);
            if (rowNum > 0) {
                jobVo.setRowNum(rowNum);
                jobIdList = autoexecJobMapper.searchJobId(jobVo);
            }
        }
        if (CollectionUtils.isNotEmpty(jobIdList)) {
            Map<String, ArrayList<Long>> operationIdMap = new HashMap<>();
            jobVoList = autoexecJobMapper.searchJob(jobIdList);
            //补充来源operation信息
            Map<Long, String> operationIdNameMap = new HashMap<>();
            List<AutoexecCombopVo> combopVoList;
            List<AutoexecScriptVersionVo> scriptVoList;
            List<AutoexecOperationVo> toolVoList;
            jobVoList.forEach(o -> {
                operationIdMap.computeIfAbsent(o.getOperationType(), k -> new ArrayList<>());
                operationIdMap.get(o.getOperationType()).add(o.getOperationId());
            });
            if (CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.COMBOP.getValue()))) {
                combopVoList = autoexecCombopMapper.getAutoexecCombopByIdList(operationIdMap.get(CombopOperationType.COMBOP.getValue()));
                combopVoList.forEach(o -> operationIdNameMap.put(o.getId(), o.getName()));
            }
            if (CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.SCRIPT.getValue()))) {
                scriptVoList = autoexecScriptMapper.getVersionByVersionIdList(operationIdMap.get(CombopOperationType.SCRIPT.getValue()));
                scriptVoList.forEach(o -> operationIdNameMap.put(o.getId(), o.getTitle()));
            }
            if (CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.TOOL.getValue()))) {
                toolVoList = autoexecToolMapper.getToolListByIdList(operationIdMap.get(CombopOperationType.TOOL.getValue()));
                toolVoList.forEach(o -> operationIdNameMap.put(o.getId(), o.getName()));
            }
            List<AutoexecJobVo> autoexecJobVos = autoexecJobMapper.getJobWarnCountAndStatus(jobIdList);
            Map<Long, AutoexecJobVo> autoexecJobVoMap = autoexecJobVos.stream().collect(toMap(AutoexecJobVo::getId, o -> o));

            Map<Long, List<AutoexecJobVo>> parentJobChildrenListMap = new HashMap<>();
            if (StringUtils.isNotBlank(jobVo.getKeyword()) && CollectionUtils.isNotEmpty(jobVoList)) {
                List<AutoexecJobVo> parentJobList = jobVoList.stream().filter(e -> e.getParentId() != null).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(parentJobList)) {
                    List<AutoexecJobVo> parentInfoJobList = autoexecJobMapper.getParentAutoexecJobListIdList(parentJobList.stream().map(AutoexecJobVo::getId).collect(Collectors.toList()));
                    if (CollectionUtils.isNotEmpty(parentInfoJobList)) {
                        parentJobChildrenListMap = parentInfoJobList.stream().collect(Collectors.toMap(AutoexecJobVo::getId, AutoexecJobVo::getChildren));
                    }
                }
            }

            //补充权限
            for (AutoexecJobVo vo : jobVoList) {
                vo.setOperationName(operationIdNameMap.get(vo.getOperationId()));
                AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(vo.getSource());
                if (jobSourceVo == null) {
                    throw new AutoexecJobSourceInvalidException(vo.getSource());
                }
                IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
                autoexecJobSourceActionHandler.getJobActionAuth(vo);
                //补充warnCount和ignore tooltips
                AutoexecJobVo jobWarnCountStatus = autoexecJobVoMap.get(vo.getId());
                if (jobWarnCountStatus != null) {
                    vo.setWarnCount(jobWarnCountStatus.getWarnCount());
                    if (jobWarnCountStatus.getStatus().contains(JobNodeStatus.IGNORED.getValue())) {
                        vo.setIsHasIgnored(1);
                    }
                }
                if (vo.getParentId() != null) {
                    vo.setChildren(parentJobChildrenListMap.get(vo.getId()));
                }
            }


        }
        return jobVoList;
    }


    @Override
    public void resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameList(Long jobId, List<String> jobPhaseNameList) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (StringUtils.equals(codedriver.framework.deploy.constvalue.JobSource.DEPLOY.getValue(), jobVo.getSource())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            List<Long> sqlIdList = iDeploySqlCrossoverMapper.getDeployJobSqlIdListByJobIdAndJobPhaseNameList(jobId, jobPhaseNameList);
            if (CollectionUtils.isNotEmpty(sqlIdList)) {
                iDeploySqlCrossoverMapper.resetDeploySqlStatusBySqlIdList(sqlIdList);
            }
        } else {
            List<Long> deleteSqlIdList = autoexecJobMapper.getJobSqlIdListByJobIdAndJobPhaseNameList(jobId, jobPhaseNameList);
            if (CollectionUtils.isNotEmpty(deleteSqlIdList)) {
                autoexecJobMapper.resetJobSqlStatusBySqlIdList(deleteSqlIdList);
            }
        }
    }

    @Override
    public void validateAutoexecJobLogEncoding(String encoding) {
        ConfigVo encodingConfig = configMapper.getConfigByKey("autoexec.job.log.encoding");
        boolean configChecked = false;
        if (encodingConfig != null) {
            String encodingConfigValue = encodingConfig.getValue();
            if (StringUtils.isNotBlank(encodingConfigValue)) {
                try {
                    configChecked = true;
                    JSONArray array = JSONArray.parseArray(encodingConfigValue);
                    if (!array.contains(encoding)) {
                        throw new AutoexecJobLogEncodingIllegalException(encoding);
                    }
                } catch (Exception ex) {
                    configChecked = false;
                    logger.error("autoexec.job.log.encoding格式非JsonArray");
                }
            }
        }
        if (!configChecked && JobLogEncoding.getJobLogEncoding(encoding) == null) {
            throw new AutoexecJobLogEncodingIllegalException(encoding);
        }
    }

    @Override
    public AutoexecJobPhaseNodeVo getNodeOperationStatus(JSONObject paramJson, boolean isNeedOperationList) {
        Long jobId = paramJson.getLong("jobId");
        AutoexecJobVo autoexecJobVo = autoexecJobMapper.getJobInfo(jobId);
        if (autoexecJobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        AutoexecJobContentVo jobContent = autoexecJobMapper.getJobContent(autoexecJobVo.getConfigHash());
        autoexecJobVo.setConfigStr(jobContent.getContent());
        List<AutoexecJobPhaseNodeOperationStatusVo> statusList = new ArrayList<>();
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/status/get";
        JSONObject statusJson = JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));
        AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(statusJson);
        if (isNeedOperationList) {
            AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(autoexecJobVo.getSource());
            if (jobSourceVo == null) {
                throw new AutoexecJobSourceInvalidException(autoexecJobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
            AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getSnapshotAutoexecCombop(autoexecJobVo);
            AutoexecCombopConfigVo config = combopVo.getConfig();
            if (config != null) {
                List<AutoexecJobPhaseOperationVo> jobOperationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(paramJson.getLong("jobId"), paramJson.getLong("phaseId"));
                Optional<AutoexecCombopPhaseVo> combopPhaseOptional = config.getCombopPhaseList().stream().filter(o -> Objects.equals(o.getName(), paramJson.getString("phase"))).findFirst();
                Map<String, String> descriptionUuidMap = new HashMap<>();
                Map<String, String> descriptionNameMap = new HashMap<>();
                if (combopPhaseOptional.isPresent()) {
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseOptional.get().getConfig();
                    if (phaseConfigVo != null) {
                        List<AutoexecCombopPhaseOperationVo> operationVoList = phaseConfigVo.getPhaseOperationList();
                        descriptionUuidMap = operationVoList.stream().collect(Collectors.toMap(AutoexecCombopPhaseOperationVo::getUuid, o -> o.getDescription() == null ? StringUtils.EMPTY : o.getDescription()));
                        descriptionNameMap = operationVoList.stream().collect(Collectors.toMap(AutoexecCombopPhaseOperationVo::getOperationName, o -> o.getDescription() == null ? StringUtils.EMPTY : o.getDescription()));
                    }
                }
                for (AutoexecJobPhaseOperationVo jobPhaseOperationVo : jobOperationVoList) {
                    String description;
                    if (descriptionUuidMap.containsKey(jobPhaseOperationVo.getUuid())) {
                        description = descriptionUuidMap.get(jobPhaseOperationVo.getUuid());
                    } else {
                        //兼容老数据
                        description = descriptionNameMap.get(jobPhaseOperationVo.getName());
                    }
                    statusList.add(new AutoexecJobPhaseNodeOperationStatusVo(jobPhaseOperationVo, statusJson, description));
                }

            }
            nodeVo.setOperationStatusVoList(statusList.stream().sorted(Comparator.comparing(AutoexecJobPhaseNodeOperationStatusVo::getSort)).collect(Collectors.toList()));
        }
        return nodeVo;
    }

    /**
     * 重置autoexec 作业节点状态
     *
     * @param jobVo      作业
     * @param nodeVoList 节点列表
     */
    @Override
    public void resetJobNodeStatus(AutoexecJobVo jobVo, List<AutoexecJobPhaseNodeVo> nodeVoList) {
        //重置mongodb node 状态
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
        }
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getCurrentPhase();
        try {
            JSONObject paramJson = new JSONObject();
            paramJson.put("jobId", jobVo.getId());
            paramJson.put("tenant", TenantContext.get().getTenantUuid());
            paramJson.put("execUser", UserContext.get().getUserUuid(true));
            paramJson.put("phaseName", currentPhaseVo.getName());
            paramJson.put("execMode", currentPhaseVo.getExecMode());
            paramJson.put("phaseNodeList", jobVo.getExecuteJobNodeVoList());
            for (RunnerMapVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/phase/node/status/reset";
                restVo = new RestVo.Builder(url, AuthenticateType.BUILDIN.getValue()).setPayload(paramJson).build();
                result = RestUtil.sendPostRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new RunnerHttpRequestException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            assert restVo != null;
            throw new RunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
    }

    /**
     * 检查runner联通性
     */
    @Override
    public void checkRunnerHealth(List<RunnerMapVo> runnerVos) {
        RestVo restVo;
        String result;
        String url;
        for (RunnerMapVo runner : runnerVos) {
            if (runner.getRunnerMapId() == null) {
                throw new RunnerMapNotMatchRunnerException(runner.getRunnerMapId());
            }
            if (StringUtils.isBlank(runner.getUrl())) {
                throw new AutoexecJobRunnerNotFoundException(runner.getRunnerMapId().toString());
            }
            url = runner.getUrl() + "api/rest/health/check";
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setConnectTimeout(5000).setReadTimeout(5000).setPayload(new JSONObject().toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                throw new RunnerConnectRefusedException(url, requestUtil.getError());
            }
            JSONObject resultJson = requestUtil.getResultJson();
            if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }

        }
    }

    /**
     * 执行组
     *
     * @param jobVo 作业
     */
    @Override
    public void executeGroup(AutoexecJobVo jobVo) {
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobRunnerListByJobIdAndGroupId(jobVo.getId(), jobVo.getExecuteJobGroupVo().getId());
        execute(jobVo, runnerVos);
    }

    /**
     * 执行组
     *
     * @param jobVo 作业
     */
    @Override
    public void executeNode(AutoexecJobVo jobVo) {
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        if (Objects.equals(jobVo.getCurrentPhase().getExecMode(), ExecMode.SQL.getValue())) {
            for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getExecuteJobNodeVoList()) {
                runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
            }
        } else {
            runnerVos = autoexecJobMapper.getJobRunnerListByJobIdAndJobNodeIdList(jobVo.getId(), jobVo.getExecuteNodeIdList());
        }
        execute(jobVo, runnerVos);
    }

    /**
     * 发起执行命令
     *
     * @param jobVo     作业
     * @param runnerVos runner列表
     */
    @Override
    public void execute(AutoexecJobVo jobVo, List<RunnerMapVo> runnerVos) {
        if (CollectionUtils.isEmpty(runnerVos)) {
            throw new RunnerNotMatchException();
        }
        //如果作业第一次或重跑，更新作业状态为running 和 作业开始时间
        if (Objects.equals(jobVo.getIsFirstFire(), 1)) {
            jobVo.setStatus(JobStatus.RUNNING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
        }
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("isNoFireNext", jobVo.getIsNoFireNext());
        paramJson.put("isFirstFire", jobVo.getIsFirstFire());
        if (CollectionUtils.isNotEmpty(jobVo.getExecuteJobPhaseList())) {
            paramJson.put("jobPhaseNameList", jobVo.getExecuteJobPhaseList().stream().map(AutoexecJobPhaseVo::getName).collect(Collectors.toList()));
        }
        if (jobVo.getExecuteJobGroupVo() != null) {
            paramJson.put("jobGroupIdList", Collections.singletonList(jobVo.getExecuteJobGroupVo().getSort()));
        }

        if (jobVo.getCurrentPhase() != null && Objects.equals(jobVo.getCurrentPhase().getExecMode(), ExecMode.SQL.getValue())) {
            paramJson.put("jobPhaseNodeSqlList", jobVo.getExecuteJobNodeVoList());
        } else {
            paramJson.put("jobPhaseResourceIdList", jobVo.getExecuteResourceIdList());
        }
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        String url = StringUtils.EMPTY;
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        try {
            for (RunnerMapVo runner : runnerVos) {
                jobVo.getEnvironment().put("RUNNER_ID", runner.getRunnerMapId());
                url = runner.getUrl() + "api/rest/job/exec";
                JSONObject passThroughEnv = jobVo.getPassThroughEnv();
                passThroughEnv.put("runnerId", runner.getRunnerMapId());
                if (jobVo.getExecuteJobGroupVo() != null) {
                    passThroughEnv.put("groupSort", jobVo.getExecuteJobGroupVo().getSort());
                }
                if (CollectionUtils.isNotEmpty(jobVo.getExecuteJobPhaseList())) {
                    passThroughEnv.put("phaseSort", jobVo.getExecuteJobPhaseList().get(0).getSort());
                }
                passThroughEnv.put("isFirstFire", jobVo.getIsFirstFire());
                paramJson.put("passThroughEnv", passThroughEnv);
                paramJson.put("environment", jobVo.getEnvironment());
                restVo = new RestVo.Builder(url, AuthenticateType.BUILDIN.getValue()).setPayload(paramJson).build();
                result = RestUtil.sendPostRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new RunnerHttpRequestException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            throw new RunnerConnectRefusedException(url + " " + result);
        }
    }
}
