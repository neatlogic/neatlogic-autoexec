/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.config.AutoexecConfig;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.crossover.IAutoexecJobCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.*;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.exception.job.AutoexecJobTargetOrRunnerNotFoundException;
import neatlogic.framework.autoexec.exception.job.JobParamNullException;
import neatlogic.framework.autoexec.exception.job.JobParamUserNameNullException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.autoexec.job.node.UpdateNodesFactory;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.RunnerStatus;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.runner.*;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.$;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;
import static neatlogic.framework.common.util.CommonUtil.distinctByKey;

/**
 * @since 2021/4/12 18:44
 **/
@Service
public class AutoexecJobServiceImpl implements AutoexecJobService, IAutoexecJobCrossoverService {
    private final Logger logger = LoggerFactory.getLogger(AutoexecJobServiceImpl.class);

    private Random r = new Random();
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
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    RunnerMapper runnerMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AutoexecJobNotSupportedService autoexecJobNotSupportedService;

    /**
     * 根据作业参数获取最终参数值
     *
     * @param paramMapping     映射信息
     * @param runTimeParamList 作业参数列表
     */
    @Override
    public String getFinalParamValue(ParamMappingVo paramMapping, List<AutoexecParamVo> runTimeParamList) {
        if (paramMapping != null && paramMapping.getValue() != null) {
            String value = paramMapping.getValue().toString();
            if (StringUtils.isNotBlank(value)) {
                if (Objects.equals(paramMapping.getMappingMode(), ParamMappingMode.CONSTANT.getValue())) {
                    return value;
                } else if (Objects.equals(paramMapping.getMappingMode(), ParamMappingMode.RUNTIME_PARAM.getValue())) {
                    for (AutoexecParamVo runtimeParam : runTimeParamList) {
                        if (Objects.equals(value, runtimeParam.getKey())) {
                            if (runtimeParam.getValue() != null && StringUtils.isNotBlank(runtimeParam.getValue().toString())) {
                                if (runtimeParam.getValue() instanceof ArrayList) {
                                    return JSON.toJSONString(runtimeParam.getValue());
                                }
                                return runtimeParam.getValue().toString();
                            } else {
                                throw new JobParamNullException(runtimeParam.getKey());
                            }
                        }
                    }
                }
            }

        }
        return StringUtils.EMPTY;
    }

    @Transactional
    @Override
    public void saveAutoexecCombopJob(AutoexecJobVo jobVo) {
        IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
        if (jobSource == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
        autoexecJobSourceActionHandler.updateInvokeJob(jobVo);
        AutoexecCombopConfigVo config = jobVo.getConfig();
        if (jobVo.getPlanStartTime().getTime() > System.currentTimeMillis() || Objects.equals(JobTriggerType.MANUAL.getValue(), jobVo.getTriggerType())) {
            jobVo.setStatus(JobStatus.READY.getValue());
        } else {
            jobVo.setStatus(JobStatus.PENDING.getValue());
        }
        //更新关联来源关系
        AutoexecJobInvokeVo invokeVo = new AutoexecJobInvokeVo(jobVo.getId(), jobVo.getInvokeId(), jobVo.getSource(), jobSource.getType(), jobVo.getRouteId());
        autoexecJobMapper.insertJobInvoke(invokeVo);
        if (StringUtils.isNotBlank(jobVo.getConfigHash())) {
            autoexecJobNotSupportedService.insertIntoJobContent(jobVo.getConfigHash(), jobVo.getConfigStr());
        }
        getFinalRuntimeParamList(jobVo.getRunTimeParamList(), jobVo.getParam());
        if (CollectionUtils.isNotEmpty(jobVo.getRunTimeParamList())) {
            for (AutoexecParamVo runtimeParam : jobVo.getRunTimeParamList()) {
                autoexecService.validateTextTypeParamValue(runtimeParam, runtimeParam.getValue());
            }
        }
        if (StringUtils.isNotBlank(jobVo.getParamHash())) {
            autoexecJobNotSupportedService.insertIntoJobContent(jobVo.getParamHash(), jobVo.getRunTimeParamListStr());
        }
        //更新父节作业的parentId,-1代表父作业
        if (jobVo.getParentId() != null) {
            AutoexecJobVo parentJobVo = autoexecJobMapper.getJobInfo(jobVo.getParentId());
            if (parentJobVo == null) {
                throw new AutoexecJobNotFoundException(jobVo.getParentId());
            }
            if (parentJobVo.getParentId() != null && parentJobVo.getParentId() != -1) {
                AutoexecJobVo grandParentJobVo = autoexecJobMapper.getJobInfo(parentJobVo.getParentId());
                if (grandParentJobVo != null && grandParentJobVo.getParentId() != null && grandParentJobVo.getParentId() != -1) {
                    throw new AutoexecJobNotSupportMultiParentException(grandParentJobVo.getId());
                }
            }
            if (parentJobVo.getParentId() == null) {
                autoexecJobMapper.updateJobParentIdById(parentJobVo.getId(), -1);
            }
        }
        autoexecJobMapper.insertJob(jobVo);
        jobVo.setIsFirstInit(1);
        //保存作业执行目标
        AutoexecCombopExecuteConfigVo combopExecuteConfigVo = config.getExecuteConfig();
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
        RunnerMapVo runnerMapVo = null;//默认使用同一个全局runner
        Date updateTime = new Date(System.currentTimeMillis());
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            RunnerMapVo phaseRunnerMapVo = null;
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
            List<AutoexecJobPhaseOperationVo> jobPhaseOperationVoList = new ArrayList<>();
            jobPhaseVo.setOperationList(jobPhaseOperationVoList);
            convertCombOperation2JobOperation(jobPhaseVo, jobPhaseVoList, combopPhaseOperationList, jobVo, preOperationNameMap);
            //jobPhaseNode
            if (isPhaseNodeNeedReInitByPreOutput(jobVo, jobPhaseVo)) {
                //如果需要上游出参作为执行目标则无需初始化执行当前阶段执行目标
                continue;
            }
            //如果是target、runnerTarget 则获取执行目标
            jobVo.setExecutePhase(jobPhaseVo);
            if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(autoexecCombopPhaseVo.getExecMode())) {
                initPhaseExecuteUserAndProtocolAndNode(jobVo, combopExecuteConfigVo, combopPhaseExecuteConfigVo, updateTime);
            } else {
                List<RunnerMapVo> runnerMapList = autoexecJobSourceActionHandler.getRunnerMapList(jobVo, combopPhaseExecuteConfigVo);
                if (CollectionUtils.isEmpty(runnerMapList)) {
                    throw new RunnerNotMatchException();
                }
                if (Objects.equals(jobVo.getExecutePhase().getRunnerGroupFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())) {
                    if (runnerMapVo == null) {
                        int runnerMapIndex = r.nextInt(runnerMapList.size());
                        runnerMapVo = runnerMapList.get(runnerMapIndex);
                        autoexecJobMapper.updateJobLocalRunnerId(jobVo.getId(), runnerMapVo.getRunnerMapId());
                    }
                    phaseRunnerMapVo = runnerMapVo;
                } else {
                    int runnerMapIndex = r.nextInt(runnerMapList.size());
                    phaseRunnerMapVo = runnerMapList.get(runnerMapIndex);
                }

                jobPhaseVo.setLcd(updateTime);
                AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), jobPhaseVo, "runner", JobNodeStatus.PENDING.getValue());
                nodeVo.setRunnerMapId(phaseRunnerMapVo.getRunnerMapId());
                autoexecJobMapper.insertJobPhaseNode(nodeVo);
                runnerMapper.insertRunnerMap(phaseRunnerMapVo);
                autoexecJobMapper.insertJobPhaseRunner(nodeVo.getJobId(), nodeVo.getJobGroupId(), nodeVo.getJobPhaseId(), nodeVo.getRunnerMapId(), nodeVo.getLcd());
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

        //初始化完清空currentPhase,因为创建作业execute无需currenPhase
        jobVo.setExecutePhase(null);

        //保存作业执行器状态
        refreshJobRunner(jobVo.getId(), null);
    }

    /**
     * 判断阶段是否需要根据"上游出参"更新节点
     *
     * @param jobVo             作业
     * @param currentJobPhaseVo 当前阶段
     * @return true|false
     */
    private boolean isPhaseNodeNeedReInitByPreOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo) {
        if (StringUtils.isBlank(jobVo.getConfigStr())) {
            AutoexecJobContentVo jobContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
            if (jobContentVo == null || StringUtils.isBlank(jobContentVo.getContent())) {
                throw new AutoexecJobConfigNotFoundException(jobVo.getId());
            }
            jobVo.setConfigStr(jobContentVo.getContent());
        }
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
                        if (executeConfigVo != null && Objects.equals(executeConfigVo.getIsPresetExecuteConfig(), 1)) {
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
            throw new AutoexecJobConfigNotFoundException(jobVo.getId());
        }
        jobVo.setConfigStr(jobContentVo.getContent());
        AutoexecCombopConfigVo combopConfigVo = jobVo.getConfig();
        if (combopConfigVo != null) {
            List<AutoexecCombopPhaseVo> combopPhaseVos = combopConfigVo.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseVos)) {
                for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVos) {
                    if (Objects.equals(combopPhaseVo.getExecMode(), ExecMode.RUNNER.getValue()) || combopPhaseVo.getGroupSort() <= currentJobPhaseVo.getJobGroupVo().getSort() || !Objects.equals(currentJobPhaseVo.getJobGroupVo().getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName()) || Objects.equals(combopPhaseVo.getName(), currentJobPhaseVo.getName())) {
                        continue;
                    }
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                    if (phaseConfigVo != null) {
                        AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                        if (executeConfigVo != null && Objects.equals(executeConfigVo.getIsPresetExecuteConfig(), 1)) {
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
        jobVo.setPreOutputPhase(currentJobPhaseVo);
        List<AutoexecCombopPhaseVo> combopPhaseVoList = new ArrayList<>();
        List<AutoexecJobPhaseVo> jobPhaseVoList = getJobPhaseListByPreOutput(jobVo, currentJobPhaseVo, combopPhaseVoList);
        //autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(jobPhaseVoList.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()), JobPhaseStatus.PENDING.getValue());
        Map<String, AutoexecJobPhaseVo> jobPhaseUuidMap = jobPhaseVoList.stream().collect(Collectors.toMap(AutoexecJobPhaseVo::getUuid, o -> o));
        Date updateTime = new Date(System.currentTimeMillis());
        for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVoList) {
            AutoexecJobPhaseVo targetPhase = jobPhaseUuidMap.get(combopPhaseVo.getUuid());
            if (targetPhase != null) {
                jobVo.setExecutePhase(targetPhase);
                initPhaseExecuteUserAndProtocolAndNode(jobVo, jobVo.getConfig().getExecuteConfig(), combopPhaseVo.getConfig(), updateTime);
            }
        }
        refreshJobRunner(jobVo.getId(), updateTime.getTime());
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
        getCombopPhaseListByPreOutput(jobVo, currentJobPhaseVo, combopPhaseVoList);
        List<AutoexecJobPhaseVo> jobPhaseVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(combopPhaseVoList)) {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseUuidList(jobVo.getId(), combopPhaseVoList.stream().map(AutoexecCombopPhaseVo::getUuid).collect(Collectors.toList()));
        }
        return jobPhaseVoList;
    }

    /**
     * 根据当前阶段出参获取需要更新执行目标的其他阶段
     *
     * @param jobVo             作业
     * @param currentJobPhaseVo 当前阶段
     * @param combopPhaseVoList 需要更新的阶段配置
     */
    private void getCombopPhaseListByPreOutput(AutoexecJobVo jobVo, AutoexecJobPhaseVo currentJobPhaseVo, List<AutoexecCombopPhaseVo> combopPhaseVoList) {
        AutoexecJobContentVo jobContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        if (jobContentVo == null || StringUtils.isBlank(jobContentVo.getContent())) {
            throw new AutoexecJobConfigNotFoundException(jobVo.getId());
        }
        jobVo.setConfigStr(jobContentVo.getContent());
        AutoexecCombopConfigVo combopConfigVo = jobVo.getConfig();
        if (combopConfigVo != null) {
            List<AutoexecCombopPhaseVo> combopPhaseVos = combopConfigVo.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseVos)) {
                for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseVos) {
                    if (combopPhaseVo.getGroupSort() <= currentJobPhaseVo.getJobGroupVo().getSort() || !Objects.equals(currentJobPhaseVo.getJobGroupVo().getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName()) || Objects.equals(combopPhaseVo.getName(), currentJobPhaseVo.getName())) {
                        continue;
                    }
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                    if (phaseConfigVo != null) {
                        AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                        if (executeConfigVo != null && Objects.equals(executeConfigVo.getIsPresetExecuteConfig(), 1)) {
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
        }
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
                // 因为组合工具草稿测试功能和自定义脚本版本测试功能的source都置为test，所以增加scriptVersionId != null判断
                // 自定义脚本版本测试功能执行if代码块逻辑，组合工具草稿测试功能执行else代码块逻辑
                if ((Objects.equals(jobVo.getSource(), JobSource.TEST.getValue()) || Objects.equals(jobVo.getSource(), JobSource.SCRIPT_TEST.getValue())) && autoexecCombopPhaseOperationVo.getScriptVersionId() != null) {
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
                jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(jobVo, autoexecCombopPhaseOperationVo, jobPhaseVo, scriptVo, scriptVersionVo, script, jobPhaseVoList, preOperationNameMap);
            } else {
                AutoexecToolVo toolVo = autoexecToolMapper.getToolById(id);
                if (toolVo == null) {
                    throw new AutoexecToolNotFoundException(id);
                }
                jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(jobVo, autoexecCombopPhaseOperationVo, jobPhaseVo, toolVo, jobPhaseVoList, preOperationNameMap);
            }
            initIfBlockOperation(autoexecCombopPhaseOperationVo, jobPhaseOperationVo, jobPhaseVo, jobPhaseVoList, jobVo, preOperationNameMap);
            initLOOPBlockOperation(autoexecCombopPhaseOperationVo, jobPhaseOperationVo, jobPhaseVo, jobPhaseVoList, jobVo, preOperationNameMap);
            jobPhaseOperationVoList.add(jobPhaseOperationVo);
            jobPhaseVo.getOperationList().add(jobPhaseOperationVo);
            autoexecJobMapper.insertJobPhaseOperation(jobPhaseOperationVo);
            if (StringUtils.isNotBlank(jobPhaseOperationVo.getParamHash())) {
                autoexecJobNotSupportedService.insertIntoJobContent(jobPhaseOperationVo.getParamHash(), jobPhaseOperationVo.getParamStr());
            }
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

    /**
     * @param autoexecCombopPhaseOperationVo 组合工具阶段工具列表
     * @param jobPhaseOperationVo            作业工具
     * @param jobPhaseVo                     当前作业阶段
     * @param jobPhaseVoList                 作业所有阶段列表
     * @param jobVo                          作业
     * @param preOperationNameMap            记录上游阶段工具uuid对应的名称
     */
    private void initLOOPBlockOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo, AutoexecJobPhaseOperationVo jobPhaseOperationVo, AutoexecJobPhaseVo jobPhaseVo, List<AutoexecJobPhaseVo> jobPhaseVoList, AutoexecJobVo jobVo, Map<String, String> preOperationNameMap) {
        AutoexecCombopPhaseOperationConfigVo combopPhaseOperationConfigVo = autoexecCombopPhaseOperationVo.getConfig();
        if (combopPhaseOperationConfigVo != null && Objects.equals(autoexecCombopPhaseOperationVo.getOperationName(), "native/LOOP-Block")) {
            String loopItems = combopPhaseOperationConfigVo.getLoopItems();
            String loopItemVar = combopPhaseOperationConfigVo.getLoopItemVar();
            if (StringUtils.isBlank(loopItems)) {
                throw new AutoexecParamValueIrregularException(jobPhaseVo.getName(), jobPhaseOperationVo.getName(), $.t("nmas.autoexec.loopitems"), "loopItems", loopItems);
            }
            if (StringUtils.isBlank(loopItemVar)) {
                throw new AutoexecParamValueIrregularException(jobPhaseVo.getName(), jobPhaseOperationVo.getName(), $.t("nmas.autoexec.loopitemvar"), "loopItemVar", loopItemVar);
            }

            JSONObject paramObj = jobPhaseOperationVo.getParam();
            paramObj.put("loopItems", loopItems);
            paramObj.put("loopItemVar", loopItemVar);
            if (CollectionUtils.isNotEmpty(combopPhaseOperationConfigVo.getOperations())) {
                List<AutoexecCombopPhaseOperationVo> operations = combopPhaseOperationConfigVo.getOperations();
                operations.forEach(o -> {
                    o.setParentOperationId(jobPhaseOperationVo.getId());
                    o.setParentOperationType("loop");
                });
                List<AutoexecJobPhaseOperationVo> loopJobOperation = convertCombOperation2JobOperation(jobPhaseVo, jobPhaseVoList, operations, jobVo, preOperationNameMap);
                paramObj.put("operations", loopJobOperation);
            }
            jobPhaseOperationVo.setParamStr(paramObj.toString());
        }
    }

    @Override
    public void initPhaseExecuteUserAndProtocolAndNode(AutoexecJobVo jobVo, AutoexecCombopExecuteConfigVo combopExecuteConfigVo, AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo, Date updateTime) {
        boolean isHasNode = false;
        boolean isPhaseConfig = false;
        boolean isGroupConfig = false;
        String userName = null;
        Long protocolId = null;
        Integer roundCount = null;
        String parallelPolicy = jobVo.getParallelPolicy();
        Integer parallelCount = null;
        AutoexecJobPhaseVo jobPhase = jobVo.getExecutePhase();
        AutoexecJobGroupVo jobGroupVo = jobVo.getExecutePhase().getJobGroupVo();
        //作业层执行用户引用作业参数且抛作业参数为空
        String jobUserNameParamNullKey = StringUtils.EMPTY;
        if (combopExecuteConfigVo != null) {
            //先获取组合工具配置的执行用户和协议
            try {
                userName = getFinalParamValue(combopExecuteConfigVo.getExecuteUser(), jobVo.getRunTimeParamList());
            } catch (JobParamNullException e) {
                jobUserNameParamNullKey = combopExecuteConfigVo.getExecuteUser().getValue().toString();
            }
            protocolId = combopExecuteConfigVo.getProtocolId();
            //兼容老数据不存在policy
            if (StringUtils.isBlank(parallelPolicy) && jobVo.getRoundCount() != null) {
                parallelPolicy = AutoexecParallelPolicy.ROUND_COUNT.getValue();
            }
            if (StringUtils.isNotBlank(userName)) {
                jobPhase.setUserNameFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            }
            if (protocolId != null) {
                jobPhase.setProtocolFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            }
            if (StringUtils.isNotBlank(parallelPolicy)) {
                jobPhase.setRoundCountFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
                if (Objects.equals(AutoexecParallelPolicy.ROUND_COUNT.getValue(), parallelPolicy)) {
                    roundCount = jobVo.getRoundCount();
                } else if (Objects.equals(AutoexecParallelPolicy.PARALLEL.getValue(), parallelPolicy)) {
                    parallelCount = jobVo.getParallelCount();
                }
                jobGroupVo.setParallelFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo;
        //判断group是不是grayScale，如果是则从group中获取执行节点、账号、执行用户
        if (Objects.equals(jobGroupVo.getPolicy(), AutoexecJobGroupPolicy.GRAYSCALE.getName())) {
            AutoexecCombopGroupConfigVo groupConfig = jobGroupVo.getConfig();
            if (groupConfig != null) {
                executeConfigVo = groupConfig.getExecuteConfig();
                //判断组执行节点是否配置
                if (executeConfigVo != null) {
                    if (executeConfigVo.getExecuteUser() != null && executeConfigVo.getExecuteUser().getValue() != null) {
                        try {
                            String userNameTmp = getFinalParamValue(executeConfigVo.getExecuteUser(), jobVo.getRunTimeParamList());
                            if (StringUtils.isNotBlank(userNameTmp)) {
                                userName = userNameTmp;
                                jobPhase.setUserNameFrom(AutoexecJobPhaseNodeFrom.GROUP.getValue());
                            }
                        } catch (JobParamNullException e) {
                            throw new JobParamUserNameNullException(jobGroupVo.getSort(), executeConfigVo.getExecuteUser().getValue());
                        }
                    } else if (StringUtils.isNotBlank(jobUserNameParamNullKey)) {
                        throw new JobParamUserNameNullException(jobUserNameParamNullKey);
                    }
                    if (executeConfigVo.getProtocolId() != null) {
                        protocolId = executeConfigVo.getProtocolId();
                        jobPhase.setProtocolFrom(AutoexecJobPhaseNodeFrom.GROUP.getValue());
                    }
                    isGroupConfig = executeConfigVo.getExecuteNodeConfig() != null && !executeConfigVo.getExecuteNodeConfig().isNull();
                    if (isGroupConfig) {
                        jobVo.setNodeFrom(AutoexecJobPhaseNodeFrom.GROUP.getValue());
                        isHasNode = getJobNodeList(executeConfigVo, jobVo, userName, protocolId, updateTime);
                    }
                    String parallelPolicyTmp = executeConfigVo.getParallelPolicy();
                    //兼容老数据不存在policy
                    if (StringUtils.isBlank(parallelPolicyTmp) && executeConfigVo.getRoundCount() != null) {
                        parallelPolicyTmp = AutoexecParallelPolicy.ROUND_COUNT.getValue();
                    }
                    if (StringUtils.isNotBlank(parallelPolicyTmp)) {
                        parallelPolicy = parallelPolicyTmp;
                        if (Objects.equals(AutoexecParallelPolicy.ROUND_COUNT.getValue(), parallelPolicy)) {
                            roundCount = executeConfigVo.getRoundCount();
                            jobGroupVo.setParallelPolicy(parallelPolicy);
                            jobGroupVo.setRoundCount(roundCount);
                        } else if (Objects.equals(AutoexecParallelPolicy.PARALLEL.getValue(), parallelPolicy)) {
                            parallelCount = executeConfigVo.getParallelCount();
                            jobGroupVo.setParallelPolicy(parallelPolicy);
                            jobGroupVo.setParallelCount(parallelCount);
                        }
                        jobGroupVo.setParallelFrom(AutoexecJobPhaseNodeFrom.GROUP.getValue());
                        jobPhase.setRoundCountFrom(AutoexecJobPhaseNodeFrom.GROUP.getValue());
                    }
                }
            }
        } else {
            executeConfigVo = combopPhaseExecuteConfigVo.getExecuteConfig();
            if (executeConfigVo != null && Objects.equals(executeConfigVo.getIsPresetExecuteConfig(), 1)) {
                if (executeConfigVo.getExecuteUser() != null && executeConfigVo.getExecuteUser().getValue() != null) {
                    try {
                        String userNameTmp = getFinalParamValue(executeConfigVo.getExecuteUser(), jobVo.getRunTimeParamList());
                        if (StringUtils.isNotBlank(userNameTmp)) {
                            userName = userNameTmp;
                            jobPhase.setUserNameFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue());
                        }
                    } catch (JobParamNullException e) {
                        throw new JobParamUserNameNullException(jobPhase.getName(), executeConfigVo.getExecuteUser().getValue());
                    }
                } else if (StringUtils.isNotBlank(jobUserNameParamNullKey)) {
                    throw new JobParamUserNameNullException(jobUserNameParamNullKey);
                }
                if (executeConfigVo.getProtocolId() != null) {
                    protocolId = executeConfigVo.getProtocolId();
                    jobPhase.setProtocolFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue());
                }
                //判断阶段执行节点是否配置
                isPhaseConfig = executeConfigVo.getExecuteNodeConfig() != null && !executeConfigVo.getExecuteNodeConfig().isNull();
                if (isPhaseConfig) {
                    jobVo.setNodeFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue());
                    isHasNode = getJobNodeList(executeConfigVo, jobVo, userName, protocolId, updateTime);
                }
                String parallelPolicyTmp = executeConfigVo.getParallelPolicy();
                //兼容老数据不存在policy
                if (StringUtils.isBlank(parallelPolicyTmp) && executeConfigVo.getRoundCount() != null) {
                    parallelPolicyTmp = AutoexecParallelPolicy.ROUND_COUNT.getValue();
                }
                if (StringUtils.isNotBlank(parallelPolicyTmp)) {
                    parallelPolicy = parallelPolicyTmp;
                    if (Objects.equals(AutoexecParallelPolicy.ROUND_COUNT.getValue(), parallelPolicy)) {
                        roundCount = executeConfigVo.getRoundCount();
                    } else if (Objects.equals(AutoexecParallelPolicy.PARALLEL.getValue(), parallelPolicy)) {
                        parallelCount = executeConfigVo.getParallelCount();
                    }
                    jobPhase.setRoundCountFrom(AutoexecJobPhaseNodeFrom.PHASE.getValue());
                }
            }
        }
        //如果阶段没有设置执行目标，则使用全局执行目标
        if (!isPhaseConfig && !isGroupConfig) {
            jobVo.setNodeFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            isHasNode = getJobNodeList(combopExecuteConfigVo, jobVo, userName, protocolId, updateTime);
        }
        //如果都找不到执行节点
        if (!isHasNode) {
            throw new AutoexecJobPhaseNodeNotFoundException(jobVo.getExecutePhase().getName(), isPhaseConfig);
        }

        jobPhase.setUserName(userName);
        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
        AccountProtocolVo protocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolId(protocolId);
        if (protocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(protocolId);
        }
        //如果并发策略是并发数量类型，则需要计算分批数
        if (Objects.equals(parallelPolicy, AutoexecParallelPolicy.PARALLEL.getValue())) {
            if (parallelCount == null) {
                throw new AutoexecParallelCountIsRequiredException();
            }
            if (parallelCount == -1 || parallelCount == 0 || parallelCount == 1) {
                roundCount = parallelCount;
            } else {
                int phaseNodeCount = autoexecJobMapper.getJobPhaseNodeCountWithoutDeleteAndInvalidByJobIdAndPhaseId(jobVo.getId(), jobPhase.getId());
                // 向上取整计算批次数
                roundCount = (phaseNodeCount + parallelCount - 1) / parallelCount;
            }
            //如果组是grayscale则需要更新对应组的roundCount
            if (Objects.equals(jobGroupVo.getPolicy(), AutoexecJobGroupPolicy.GRAYSCALE.getName())) {
                jobGroupVo.setParallelPolicy(AutoexecParallelPolicy.PARALLEL.getValue());
                jobGroupVo.setRoundCount(roundCount);
                jobGroupVo.setParallelCount(parallelCount);
            }
        } else {
            //如果组是grayscale则需要更新对应组的roundCount
            if (Objects.equals(jobGroupVo.getPolicy(), AutoexecJobGroupPolicy.GRAYSCALE.getName())) {
                jobGroupVo.setParallelPolicy(AutoexecParallelPolicy.ROUND_COUNT.getValue());
                jobGroupVo.setRoundCount(roundCount);
            }
        }

        if (roundCount == null) {
            throw new AutoexecRoundCountIsRequiredException();
        }
        jobPhase.setProtocol(protocolVo.getName());
        jobPhase.setRoundCount(roundCount);
        jobPhase.setParallelPolicy(parallelPolicy);
        jobPhase.setParallelCount(parallelCount);
        jobPhase.setNodeFrom(jobVo.getNodeFrom());

        //跟新节点来源
        autoexecJobMapper.updateJobPhaseFrom(jobPhase);

    }

    @Override
    public void refreshJobParam(Long jobId, JSONObject paramJson) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (!Objects.equals(CombopOperationType.COMBOP.getValue(), jobVo.getOperationType())) {
            throw new AutoexecJobPhaseOperationMustBeCombopException();
        }
        //补充运行参数真实的值
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(jobVo.getOperationId());
        if (activeVersionId == null) {
            return;
        }
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(activeVersionId);
        if (autoexecCombopVersionVo == null) {
            return;
        }
        AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
        if (versionConfig == null) {
            return;
        }
        List<AutoexecParamVo> runtimeParamList = versionConfig.getRuntimeParamList();
        if (runtimeParamList == null) {
            runtimeParamList = new ArrayList<>();
        }
        getFinalRuntimeParamList(runtimeParamList, paramJson);
        jobVo.setRunTimeParamList(runtimeParamList);
        for (AutoexecParamVo runtimeParam : runtimeParamList) {
            autoexecService.validateTextTypeParamValue(runtimeParam, runtimeParam.getValue());
        }
        if (StringUtils.isNotBlank(jobVo.getParamHash())) {
            autoexecJobNotSupportedService.insertIntoJobContent(jobVo.getParamHash(), jobVo.getRunTimeParamListStr());
            autoexecJobMapper.updateJobParamHashById(jobVo.getId(), jobVo.getParamHash());
        } else {
            autoexecJobMapper.updateJobParamHashById(jobVo.getId(), null);
        }

    }

    private void getFinalRuntimeParamList(List<AutoexecParamVo> runTimeParamList, JSONObject param) {
        if (MapUtils.isEmpty(param)) {
            return;
        }
        if (CollectionUtils.isNotEmpty(runTimeParamList)) {
            for (AutoexecParamVo paramVo : runTimeParamList) {
                if (paramVo != null) {
                    Object value = param.get(paramVo.getKey());
                    paramVo.setValue(value);
                }
            }
        }
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, List<AutoexecJobPhaseVo> jobPhaseVoList) {
        refreshJobPhaseNodeList(jobId, jobPhaseVoList, null);
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, List<AutoexecJobPhaseVo> jobPhaseVoList, AutoexecCombopExecuteConfigVo combopExecuteConfigVo) {
//        AutoexecCombopExecuteConfigVo combopExecuteConfigVo = null;
        //优先使用传进来的执行节点
//        if (MapUtils.isNotEmpty(executeConfig)) {
//            combopExecuteConfigVo = JSON.toJavaObject(executeConfig, AutoexecCombopExecuteConfigVo.class);
//        }
        Date updateTime = new Date();
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        jobVo.setConfigStr(autoexecJobMapper.getJobContent(jobVo.getConfigHash()).getContent());
        //重跑获取已存在节点的resourceId -> runnerMapId
        List<AutoexecJobPhaseNodeVo> nodeList = autoexecJobMapper.getJobPhaseNodeListWithRunnerByJobId(jobId);
        jobVo.setNodeResourceIdRunnerIdMap(nodeList.stream().filter(o -> o.getResourceId() != null).filter(distinctByKey(AutoexecJobPhaseNodeVo::getResourceId)).collect(Collectors.toMap(AutoexecJobPhaseNodeVo::getResourceId, AutoexecJobPhaseNodeVo::getRunnerMapId)));
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
                if (isPhaseNodeNeedReInitByPreOutput(jobVo, jobPhaseVo)) {
                    //如果需要上游出参作为执行目标则无需初始化执行当前阶段执行目标
                    autoexecJobMapper.updateJobPhaseNodeStatusByJobPhaseIdAndIsDelete(jobPhaseVo.getId(), JobNodeStatus.PENDING.getValue(), 0);
                    refreshPhaseRunnerStatus(jobPhaseVo);
                    continue;
                }
                jobPhaseVo.setCombopId(jobVo.getOperationId());
                jobVo.setExecutePhase(jobPhaseVo);
                initPhaseExecuteUserAndProtocolAndNode(jobVo, combopExecuteConfigVo, combopPhaseExecuteConfigVo, updateTime);
                refreshPhaseRunnerStatus(jobPhaseVo);
            }
        }
        //update runnerPhaseNodeStatus
        List<AutoexecCombopPhaseVo> combopRunnerPhaseList = configVo.getCombopPhaseList().stream().filter(o -> Objects.equals(ExecMode.RUNNER.getValue(), o.getExecMode())).collect(Collectors.toList());
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopRunnerPhaseList) {
            Optional<AutoexecJobPhaseVo> jobPhaseVoOptional = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getName(), autoexecCombopPhaseVo.getName())).findFirst();
            if (jobPhaseVoOptional.isPresent()) {
                autoexecJobMapper.updateJobPhaseNodeStatusByJobPhaseIdAndIsDelete(jobPhaseVoOptional.get().getId(), JobNodeStatus.PENDING.getValue(), 0);
                autoexecJobMapper.updateJobPhaseRunnerStatusByJobIdAndPhaseId(jobId, jobPhaseVoOptional.get().getId(), JobNodeStatus.PENDING.getValue());
            }
        }
        refreshJobRunner(jobId, updateTime.getTime());
    }

    @Override
    public void refreshJobNodeList(Long jobId) {
        refreshJobNodeList(jobId, null);
    }

    @Override
    public void refreshJobNodeList(Long jobId, AutoexecCombopExecuteConfigVo executeConfig) {
        List<AutoexecJobPhaseVo> phaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobId);
        refreshJobPhaseNodeList(jobId, phaseVoList, executeConfig);
    }

    @Override
    public void getAutoexecJobDetail(AutoexecJobVo jobVo) {
        AutoexecJobContentVo paramContentVo = null;
        if (StringUtils.isNotBlank(jobVo.getParamHash())) {
            paramContentVo = autoexecJobMapper.getJobContent(jobVo.getParamHash());
        }
        if (paramContentVo != null && StringUtils.isNotBlank(paramContentVo.getContent())) {
            jobVo.setRunTimeParamList(JSON.parseArray(paramContentVo.getContent(), AutoexecParamVo.class));
        }
        AutoexecJobContentVo jobContent = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        if (jobContent == null) {
            throw new AutoexecJobConfigNotFoundException(jobVo.getId());
        }
        AutoexecJobInvokeVo invokeVo = autoexecJobMapper.getJobInvokeByJobId(jobVo.getId());
        if (invokeVo != null) {
            jobVo.setInvokeId(invokeVo.getInvokeId());
        }
        jobVo.setConfigStr(jobContent.getContent());
        List<AutoexecJobPhaseVo> jobPhaseVoList = jobVo.getPhaseList();
        AutoexecJobGroupVo executeJobGroupVo = jobVo.getExecuteJobGroupVo();
        if (executeJobGroupVo != null) {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobVo.getId(), executeJobGroupVo.getSort());
        }
        List<AutoexecJobGroupVo> jobGroupVos = autoexecJobMapper.getJobGroupByJobId(jobVo.getId());
        Map<Long, AutoexecJobGroupVo> jobGroupIdMap = jobGroupVos.stream().collect(Collectors.toMap(AutoexecJobGroupVo::getId, e -> e));
        if (CollectionUtils.isNotEmpty(jobPhaseVoList)) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getSnapshotAutoexecCombop(jobVo);
            Map<String, String> descriptionMap = new HashMap<>();
            for (AutoexecCombopPhaseVo combopPhase : combopVo.getConfig().getCombopPhaseList()) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhase.getConfig();
                if (phaseConfigVo != null) {
                    List<AutoexecCombopPhaseOperationVo> operationVoList = phaseConfigVo.getPhaseOperationList();
                    descriptionMap.putAll(operationVoList.stream().collect(Collectors.toMap(AutoexecCombopPhaseOperationVo::getUuid, o -> o.getDescription() == null ? StringUtils.EMPTY : o.getDescription())));
                }
            }
            List<AutoexecJobPhaseVo> jobPhaseWithOperationList = autoexecJobMapper.getJobPhaseListWithOperationWithoutParentByJobId(jobVo.getId());
            Map<Long, List<AutoexecJobPhaseOperationVo>> phaseOperationVoMap = jobPhaseWithOperationList.stream().collect(Collectors.toMap(AutoexecJobPhaseVo::getId, AutoexecJobPhaseVo::getOperationList));
            //批量获取operationParamContent
            List<String> paramContentHashList = new ArrayList<>();
            Map<String, AutoexecJobContentVo> paramContentMap = new HashMap<>();
            for (Map.Entry<Long, List<AutoexecJobPhaseOperationVo>> entry : phaseOperationVoMap.entrySet()) {
                List<AutoexecJobPhaseOperationVo> operationVos = entry.getValue();
                for (AutoexecJobPhaseOperationVo operationVo : operationVos) {
                    if (StringUtils.isNotBlank(operationVo.getParamHash())) {
                        paramContentHashList.add(operationVo.getParamHash());
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(paramContentHashList)) {
                List<AutoexecJobContentVo> paramContentVos = autoexecJobMapper.getJobContentList(paramContentHashList);
                if (CollectionUtils.isNotEmpty(paramContentVos)) {
                    paramContentMap = paramContentVos.stream().collect(Collectors.toMap(AutoexecJobContentVo::getHash, o -> o));
                }
            }
            for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
                phaseVo.setJobGroupVo(jobGroupIdMap.get(phaseVo.getGroupId()));
                List<AutoexecJobPhaseOperationVo> operationVoList = phaseOperationVoMap.get(phaseVo.getId());
                phaseVo.setOperationList(operationVoList);
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    paramContentVo = paramContentMap.get(operationVo.getParamHash());
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
        if (jobVo.getExecutePhase().getCurrentNode() != null) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            //确保已存在的资产使用同一个runner
            Long runnerMapId = jobVo.getNodeResourceIdRunnerIdMap().get(jobVo.getExecutePhase().getCurrentNode().getResourceId());
            if (runnerMapId != null) {
                return runnerMapId;
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            List<RunnerMapVo> runnerMapVos = autoexecJobSourceActionHandler.getRunnerMapList(jobVo);
            if (CollectionUtils.isNotEmpty(runnerMapVos)) {
                int runnerMapIndex = (int) (jobVo.getExecutePhase().getCurrentNode().getId() % runnerMapVos.size());
                RunnerMapVo runnerMapVo = runnerMapVos.get(runnerMapIndex);
                if (runnerMapVo.getRunnerMapId() == null) {
                    runnerMapVo.setRunnerMapId(runnerMapVo.getId());
                    runnerMapper.insertRunnerMap(runnerMapVo);
                }
                jobVo.getNodeResourceIdRunnerIdMap().put(jobVo.getExecutePhase().getCurrentNode().getResourceId(), runnerMapVo.getRunnerMapId());
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
    private boolean getJobNodeList(AutoexecCombopExecuteConfigVo combopExecuteConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId, Date updateTime) {
        //执行用户不能为空
        if (StringUtils.isBlank(userName)) {
            logger.error("autoexec job username is blank!");
            throw new AutoexecUserNameNotFoundException();
        }
        if (combopExecuteConfigVo == null) {
            return false;
        }
        jobVo.getExecutePhase().setLcd(updateTime);

        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = combopExecuteConfigVo.getExecuteNodeConfig();
        boolean isHasNode = UpdateNodesFactory.updateNodes(executeNodeConfigVo, jobVo, userName, protocolId);
        logger.debug("##AfterUpdateNodes:-------------------------------------------------------------------------------start");
        //long ccc = System.currentTimeMillis();
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getExecutePhase();
        //检查当前阶段是否需要更新别的阶段执行目标，如果是则该阶段只能存在一个节点
        if (jobPhaseVo.getIsPreOutputUpdateNode() == 1) {
            int nodeCount = autoexecJobMapper.searchJobPhaseNodeCount(new AutoexecJobPhaseNodeVo(jobPhaseVo.getId(), 0));
            List<AutoexecCombopPhaseVo> combopPhaseVoList = new ArrayList<>();
            getCombopPhaseListByPreOutput(jobVo, jobPhaseVo, combopPhaseVoList);
            if (nodeCount != 1) {
                throw new AutoexecJobUpdateNodeByPreOutPutListException(jobPhaseVo, combopPhaseVoList);
            }
        }
        boolean isNeedLncd = true;//用于判断是否需要更新lncd（用于判断是否需要重新下载节点）
        if (jobVo.getIsFirstInit() == 0) {
            //删除没有跑过的历史节点
            logger.debug("##deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus:-------------------------------------------------------------------------------start");
            //long deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus = System.currentTimeMillis();
            Integer deleteCount = autoexecJobMapper.deleteJobPhaseNodeByJobPhaseIdAndUpdateTagAndStatus(jobPhaseVo.getId(), updateTime.getTime(), JobNodeStatus.PENDING.getValue());
            //System.out.println((System.currentTimeMillis() - deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus) + " ##deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus:-------------------------------------------------------------------------------");
            logger.debug("##deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus:-------------------------------------------------------------------------------end");

            isNeedLncd = deleteCount > 0;
            //更新该阶段所有不是最近更新的节点为已删除，即非法历史节点
            //long cvv = System.currentTimeMillis();
            logger.debug("##updateJobPhaseNodeIsDeleteByJobPhaseIdAndUpdateTag:-------------------------------------------------------------------------------start");
            Integer updateCount = autoexecJobMapper.updateJobPhaseNodeIsDeleteByJobPhaseIdAndUpdateTag(jobPhaseVo.getId(), updateTime.getTime());
            //System.out.println((System.currentTimeMillis() - cvv) + " ##cvv:-------------------------------------------------------------------------------");
            logger.debug("##updateJobPhaseNodeIsDeleteByJobPhaseIdAndUpdateTag:-------------------------------------------------------------------------------end");
            isNeedLncd = isNeedLncd || updateCount > 0;

            //其它数据源处理数据
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            if (autoexecJobSourceActionHandler != null) {
                autoexecJobSourceActionHandler.handleDeleteJobPhaseNodeEvent(jobPhaseVo.getId(), updateTime.getTime());
            }
        }
        //阶段节点被真删除||伪删除（is_delete=1），则更新上一次修改日期(plcd),需重新下载
        if (isNeedLncd) {
            if (Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), jobVo.getNodeFrom())) {
                autoexecJobMapper.updateJobLncdById(jobVo.getId(), updateTime);
            } else if (Objects.equals(AutoexecJobPhaseNodeFrom.GROUP.getValue(), jobVo.getNodeFrom())) {
                autoexecJobMapper.updateJobGroupLncdById(jobVo.getExecuteJobGroupVo().getId(), updateTime);
            } else {
                autoexecJobMapper.updateJobPhaseLncdById(jobPhaseVo.getId(), updateTime);
            }
        }
        //更新最近一次修改时间lcd
        autoexecJobMapper.updateJobPhaseLcdById(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        //更新phase runner
        logger.debug("##refreshPhaseRunnerList:-------------------------------------------------------------------------------start");
        //long refreshPhaseRunnerList = System.currentTimeMillis();
        refreshPhaseRunnerList(jobPhaseVo);
        logger.debug("##refreshPhaseRunnerList:-------------------------------------------------------------------------------end");
        //System.out.println((System.currentTimeMillis() - refreshPhaseRunnerList) + " ##refreshPhaseRunnerList:-------------------------------------------------------------------------------");
        //System.out.println((System.currentTimeMillis() - ccc) + " ##ccc:-------------------------------------------------------------------------------");
        logger.debug("##AfterUpdateNodes:-------------------------------------------------------------------------------end");
        return isHasNode;
    }

    /**
     * 刷新作业阶段runner
     *
     * @param jobPhaseVo 作业阶段
     */
    @Override
    public void refreshPhaseRunnerList(AutoexecJobPhaseVo jobPhaseVo) {
        if (jobPhaseVo.getJobId() == null) {
            throw new AutoexecJobNotFoundException(jobPhaseVo.getJobId());
        }
        List<RunnerMapVo> jobPhaseNodeRunnerList = autoexecJobMapper.getJobPhaseNodeRunnerListByJobPhaseId(jobPhaseVo.getId());
        List<RunnerMapVo> originPhaseRunnerVoList = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobPhaseVo.getJobId(), Collections.singletonList(jobPhaseVo.getId()));
        List<RunnerMapVo> deleteRunnerList = originPhaseRunnerVoList.stream().filter(o -> jobPhaseNodeRunnerList.stream().noneMatch(j -> Objects.equals(o.getRunnerMapId(), j.getRunnerMapId()))).collect(Collectors.toList());
        for (RunnerMapVo deleteRunnerVo : deleteRunnerList) {
            autoexecJobMapper.deleteJobPhaseRunnerByJobPhaseIdAndRunnerMapId(jobPhaseVo.getId(), deleteRunnerVo.getRunnerMapId());
        }
        List<RunnerMapVo> insertRunnerList = jobPhaseNodeRunnerList.stream().filter(j -> j != null && originPhaseRunnerVoList.stream().noneMatch(o -> Objects.equals(o.getRunnerMapId(), j.getRunnerMapId()))).collect(Collectors.toList());
        for (RunnerMapVo insertRunnerVo : insertRunnerList) {
            autoexecJobMapper.insertJobPhaseRunner(jobPhaseVo.getJobId(), jobPhaseVo.getGroupId(), jobPhaseVo.getId(), insertRunnerVo.getRunnerMapId(), jobPhaseVo.getLcd());
        }
    }

    /**
     * 获取resourceSearch,补充opType操作类型
     *
     * @param jobVo 作业
     */
    @Override
    public ResourceSearchVo getResourceSearchVoWithCmdbGroupType(AutoexecJobVo jobVo) {
        return getResourceSearchVoWithCmdbGroupType(jobVo, null);
    }

    /**
     * 获取resourceSearch,补充opType操作类型
     *
     * @param jobVo 作业
     */

    @Override
    public ResourceSearchVo getResourceSearchVoWithCmdbGroupType(AutoexecJobVo jobVo, JSONObject filterJson) {
        if (MapUtils.isEmpty(filterJson)) {
            filterJson = new JSONObject();
        }
        if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            if (combopVo != null) {
                filterJson.put("cmdbGroupType", combopVo.getOpType());
            }
        }
        IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceSearchVo searchVo = resourceCrossoverService.assembleResourceSearchVo(filterJson);
        resourceCrossoverService.handleBatchSearchList(searchVo);
        resourceCrossoverService.setIpFieldAttrIdAndNameFieldAttrId(searchVo);
        return searchVo;
    }


    /**
     * 跟新作业阶段阶段
     *
     * @param jobVo          作业
     * @param resourceVoList 最新阶段资产列表
     * @param userName       账号
     * @param protocolId     协议id
     */
    @Override
    public void updateJobPhaseNode(AutoexecJobVo jobVo, List<ResourceVo> resourceVoList, String userName, Long protocolId) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getExecutePhase();
        List<AutoexecJobPhaseNodeVo> nodeList = new ArrayList<>();
        //List<AutoexecJobPhaseNodeRunnerVo> nodeRunnerList = new ArrayList<>();
        //List<Long> resourceIdList = new ArrayList<>();
        //boolean isNeedLncd;//用于判断是否需要更新lncd（用于判断是否需要重新下载节点）
        //新增节点需重新下载
        List<Long> resourceIdList = resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList());
//        List<AutoexecJobPhaseNodeVo> originNodeList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdList(jobPhaseVo.getId(), resourceIdList);
//        isNeedLncd = originNodeList.size() != resourceVoList.size();
//        //恢复删除节点需重新下载
//        if (!isNeedLncd) {
//            List<AutoexecJobPhaseNodeVo> originDeleteNodeList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdListAndIsDelete(jobPhaseVo.getId(), resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList()));
//            isNeedLncd = !originDeleteNodeList.isEmpty();
//        }
//        if (isNeedLncd) {
//            //重新下载
//            autoexecJobMapper.updateJobPhaseLncdById(jobPhaseVo.getId(), jobPhaseVo.getLcd());
//        }
        resourceVoList.forEach(resourceVo -> {
            AutoexecJobPhaseNodeVo jobPhaseNodeVo;
            // Optional<AutoexecJobPhaseNodeVo> jobPhaseNodeVoOptional = originNodeList.stream().filter(o -> Objects.equals(o.getResourceId(), resourceVo.getId())).findFirst();
            //if (!jobPhaseNodeVoOptional.isPresent()) {
            jobPhaseNodeVo = new AutoexecJobPhaseNodeVo(resourceVo, jobPhaseVo.getJobId(), jobPhaseVo, JobNodeStatus.PENDING.getValue(), userName, protocolId);
            jobPhaseVo.setCurrentNode(jobPhaseNodeVo);
            try {
                jobPhaseNodeVo.setRunnerMapId(getRunnerByTargetIp(jobVo));
            } catch (IPIsIncorrectException e) {
                jobPhaseNodeVo.setErrorType(AutoexecJobPhaseNodeErrorType.IP_INVALID.getValue());
                jobPhaseNodeVo.setStatus(JobNodeStatus.INVALID.getValue());
            }
            if (jobPhaseNodeVo.getErrorType() == null && jobPhaseNodeVo.getRunnerMapId() == null) {
                jobPhaseNodeVo.setErrorType(AutoexecJobPhaseNodeErrorType.RUNNER_NOT_MATCH.getValue());
                jobPhaseNodeVo.setStatus(JobNodeStatus.INVALID.getValue());
            }
//            } else {
//                jobPhaseNodeVo = jobPhaseNodeVoOptional.get();
//                jobPhaseNodeVo.setLcd(jobPhaseVo.getLcd());
//            if (Boolean.TRUE.equals(isResetNode)) {
//                jobPhaseNodeVo.setStatus(JobNodeStatus.PENDING.getValue());
//            }
//            }
            nodeList.add(jobPhaseNodeVo);
            //nodeRunnerList.add(new AutoexecJobPhaseNodeRunnerVo(jobPhaseNodeVo));
            //如果大于 0,说明存在旧数据
//            Integer result = autoexecJobMapper.updateJobPhaseNodeByJobIdAndPhaseIdAndResourceId(jobPhaseNodeVo);
//            if (result == null || result == 0) {
//                autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
//                //防止旧resource 所以ignore insert
//                autoexecJobMapper.insertIgnoreJobPhaseNodeRunner(new AutoexecJobPhaseNodeRunnerVo(jobPhaseNodeVo));
//            }
            //resourceIdList.add(resourceVo.getId());
        });

        if (jobVo.getIsFirstInit() == 1) {
            autoexecJobMapper.batchInsertJobPhaseNode(nodeList);
        } else {
            autoexecJobMapper.updateJobPhaseNodeBatch(jobPhaseVo.getId(), resourceIdList, JobNodeStatus.PENDING.getValue(), jobPhaseVo.getLcd().getTime());
            autoexecJobMapper.batchInsertIgnoreJobPhaseNode(nodeList);
        }


        //其它数据源处理数据
        IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
        if (jobSource == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
        if (autoexecJobSourceActionHandler != null) {
            autoexecJobSourceActionHandler.handleAddJobPhaseNodeEvent(jobVo, nodeList, userName, protocolId, jobPhaseVo.getLcd().getTime());
        }
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
        if (Objects.equals(JobStatus.READY.getValue(), jobStatusOld) || ((Objects.equals(JobStatus.COMPLETED.getValue(), jobStatusOld) && Objects.equals(JobStatus.COMPLETED.getValue(), jobVo.getStatus())) || (Objects.equals(JobStatus.ABORTED.getValue(), jobStatusOld) && Objects.equals(JobStatus.ABORTED.getValue(), jobVo.getStatus())) || (Objects.equals(JobStatus.FAILED.getValue(), jobStatusOld) && Objects.equals(JobStatus.FAILED.getValue(), jobVo.getStatus()))) && jobPhaseVoList.stream().noneMatch(o -> Objects.equals(JobPhaseStatus.RUNNING.getValue(), o.getStatus()))) {
            paramObj.put("isRefresh", 0);
        }
    }

    @Override
    public void deleteJob(AutoexecJobVo jobVo) {
        //删除jobContentHash
        Set<String> hashSet = new HashSet<>();
        if (StringUtils.isNotBlank(jobVo.getParamHash())) {
            hashSet.add(jobVo.getParamHash());
        }
        if (StringUtils.isNotBlank(jobVo.getConfigHash())) {
            hashSet.add(jobVo.getConfigHash());
        }
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobId(jobVo.getId());
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            if (StringUtils.isNotBlank(operationVo.getParamHash())) {
                hashSet.add(operationVo.getParamHash());
            }
        }
        for (String hash : hashSet) {
            AutoexecJobContentReferenceVo autoexecJobContentReferenceVo = autoexecJobMapper.getHashUseByOtherCount(jobVo.getId(), hash);
            if (autoexecJobContentReferenceVo != null && !autoexecJobContentReferenceVo.isReferenced() && StringUtils.isNotBlank(hash)) {
                autoexecJobMapper.deleteJobContentByHash(hash);
            }
        }
        //else
        Long jobId = jobVo.getId();
        IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
        if (jobSource != null) {
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            if (autoexecJobSourceActionHandler != null) autoexecJobSourceActionHandler.deleteJob(jobVo);
        }
        autoexecJobMapper.deleteJobEvnByJobId(jobId);
        autoexecJobMapper.deleteJobGroupByJobId(jobId);
        autoexecJobMapper.deleteJobInvokeByJobId(jobId);
        autoexecJobMapper.deleteJobResourceInspectByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseOperationByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseByJobId(jobId);
        autoexecJobMapper.deleteJobRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobExecByJobId(jobId);
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
            jobVoList = autoexecJobMapper.searchJob(jobIdList, jobVo);
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
            //TODO 性能问题 待优化
            //List<AutoexecJobVo> autoexecJobVos = autoexecJobMapper.getJobWarnCountAndStatus(jobIdList);
            //Map<Long, AutoexecJobVo> autoexecJobVoMap = autoexecJobVos.stream().collect(toMap(AutoexecJobVo::getId, o -> o));

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

            Map<String, Set<String>> sourceKeyInvokeIdSetMap = new HashMap<>();
            Map<Long, String> jobIdToRouteIdMap = new HashMap<>();
            List<AutoexecJobInvokeVo> jobInvokeList = autoexecJobMapper.getJobInvokeListByJobIdList(jobIdList);
            for (AutoexecJobInvokeVo jobInvokeVo : jobInvokeList) {
                if (jobInvokeVo.getRouteId() != null) {
                    sourceKeyInvokeIdSetMap.computeIfAbsent(jobInvokeVo.getSource(), key -> new HashSet<>()).add(jobInvokeVo.getRouteId());
                }
                jobIdToRouteIdMap.put(jobInvokeVo.getJobId(), jobInvokeVo.getRouteId());
            }
            Map<String, AutoexecJobRouteVo> routeMap = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : sourceKeyInvokeIdSetMap.entrySet()) {
                IAutoexecJobSource sourceHandler = AutoexecJobSourceFactory.getHandler(entry.getKey());
                if (sourceHandler == null) {
                    continue;
                }
                List<AutoexecJobRouteVo> list = sourceHandler.getListByUniqueKeyList(new ArrayList<>(entry.getValue()));
                if (CollectionUtils.isNotEmpty(list)) {
                    for (AutoexecJobRouteVo jobRouteVo : list) {
                        routeMap.put(jobRouteVo.getId().toString(), jobRouteVo);
                    }
                }
            }
            //补充权限
            for (AutoexecJobVo vo : jobVoList) {
                vo.setOperationName(operationIdNameMap.get(vo.getOperationId()));
                IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(vo.getSource());
                if (jobSource == null) {
                    throw new AutoexecJobSourceInvalidException(vo.getSource());
                }
                IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
                if (autoexecJobSourceActionHandler != null) {
                    autoexecJobSourceActionHandler.getJobActionAuth(vo);
                }
                //补充warnCount和ignore tooltips
//                AutoexecJobVo jobWarnCountStatus = autoexecJobVoMap.get(vo.getId());
//                if (jobWarnCountStatus != null) {
//                    vo.setWarnCount(jobWarnCountStatus.getWarnCount());
//                    if (jobWarnCountStatus.getStatus().contains(JobNodeStatus.IGNORED.getValue())) {
//                        vo.setIsHasIgnored(1);
//                    }
//                }
                if (vo.getParentId() != null) {
                    vo.setChildren(parentJobChildrenListMap.get(vo.getId()));
                }
                String routeId = jobIdToRouteIdMap.get(vo.getId());
                if (routeId != null) {
                    vo.setRouteId(routeId);
                    AutoexecJobRouteVo routeVo = routeMap.get(routeId);
                    if (routeVo != null) {
                        vo.setRoute(routeVo);
                    }
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
        if (StringUtils.equals(neatlogic.framework.deploy.constvalue.JobSource.DEPLOY.getValue(), jobVo.getSource())) {
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
        boolean configChecked = false;
        String encodingConfigValue = ConfigManager.getConfig(AutoexecTenantConfig.AUTOEXEC_JOB_LOG_ENCODING);
        if (StringUtils.isNotBlank(encodingConfigValue)) {
            try {
                configChecked = true;
                JSONArray array = JSONArray.parseArray(encodingConfigValue);
                if (!array.contains(encoding)) {
                    throw new AutoexecJobLogEncodingIllegalException(encoding);
                }
            } catch (Exception ex) {
                configChecked = false;
                logger.error("nmaaj.listautoexecjoblogencodingapi.mydoservice.error");
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
        JSONObject statusJson = JSON.parseObject(AutoexecUtil.requestRunner(url, paramJson));
        AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(statusJson);

        if (isNeedOperationList) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(autoexecJobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(autoexecJobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getSnapshotAutoexecCombop(autoexecJobVo);
            AutoexecCombopConfigVo config = combopVo.getConfig();
            if (config != null) {
                List<AutoexecJobPhaseOperationVo> jobOperationVoList = autoexecJobMapper.getJobPhaseOperationListWithVersionWithoutParentByJobIdAndPhaseId(paramJson.getLong("jobId"), paramJson.getLong("phaseId"));
                Optional<AutoexecCombopPhaseVo> combopPhaseOptional = config.getCombopPhaseList().stream().filter(o -> Objects.equals(o.getName(), paramJson.getString("phase"))).findFirst();
                Map<String, AutoexecCombopPhaseOperationVo> combopOperationUuidMap = new HashMap<>();
                Map<String, AutoexecCombopPhaseOperationVo> combopOperationNameMap = new HashMap<>();
                if (combopPhaseOptional.isPresent()) {
                    AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseOptional.get().getConfig();
                    if (phaseConfigVo != null) {
                        List<AutoexecCombopPhaseOperationVo> operationVoList = phaseConfigVo.getPhaseOperationList();
                        combopOperationUuidMap = operationVoList.stream().collect(toMap(AutoexecCombopPhaseOperationVo::getUuid, o -> o));
                        combopOperationNameMap = operationVoList.stream().filter(distinctByKey(AutoexecCombopPhaseOperationVo::getOperationName)).collect(toMap(AutoexecCombopPhaseOperationVo::getOperationName, o -> o));
                    }
                }

                //批量查找入参
                /*List<AutoexecOperationVo> operationVos = new ArrayList<>();
                List<AutoexecJobPhaseOperationVo> scriptList = new ArrayList<>();
                List<Long> toolIdList = new ArrayList<>();
                for (AutoexecJobPhaseOperationVo jobPhaseOperationVo : jobOperationVoList) {
                    if (Objects.equals(CombopOperationType.SCRIPT.getValue(), jobPhaseOperationVo.getType())) {
                        scriptList.add(jobPhaseOperationVo);
                    } else {
                        toolIdList.add(jobPhaseOperationVo.getId());
                    }
                }
                if (CollectionUtils.isNotEmpty(scriptList)) {
                    operationVos.addAll(autoexecScriptMapper.getAutoexecOperationInputParamList(scriptList));
                }
                if (CollectionUtils.isNotEmpty(toolIdList)) {
                    operationVos.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
                }
                List<Long> hasInputParamOperation = operationVos.stream().filter(o -> CollectionUtils.isNotEmpty(o.getInputParamList())).map(AutoexecOperationVo::getId).collect(Collectors.toList());*/
                //找出所有作业子operationList
                List<AutoexecJobPhaseOperationVo> jobSonOperationList = autoexecJobMapper.getJobPhaseOperationListWithVersionAndParentByJobIdAndPhaseId(paramJson.getLong("jobId"), paramJson.getLong("phaseId"));
                for (AutoexecJobPhaseOperationVo jobPhaseOperationVo : jobOperationVoList) {
                    String description;
                    if (combopOperationUuidMap.containsKey(jobPhaseOperationVo.getUuid())) {
                        description = combopOperationUuidMap.get(jobPhaseOperationVo.getUuid()).getDescription();
                    } else {
                        //兼容老数据
                        description = combopOperationNameMap.get(jobPhaseOperationVo.getName()).getDescription();
                    }
                    statusList.add(new AutoexecJobPhaseNodeOperationStatusVo(jobPhaseOperationVo, statusJson, description, jobSonOperationList, combopOperationUuidMap));
                }

            }
            nodeVo.setOperationStatusVoList(statusList.stream().sorted(Comparator.comparing(AutoexecJobPhaseNodeOperationStatusVo::getSort)).collect(toList()));
        }
        return nodeVo;
    }

    /**
     * 更新阶段runner状态
     *
     * @param currentPhase 作业阶段
     */
    public void refreshPhaseRunnerStatus(AutoexecJobPhaseVo currentPhase) {
        List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVoList = autoexecJobMapper.getJobPhaseRunnerStatusByNodeStatus(currentPhase.getJobId(), currentPhase.getId());
        for (AutoexecJobPhaseRunnerVo jobPhaseRunnerVo : jobPhaseRunnerVoList) {
            List<String> statusList = Arrays.stream(jobPhaseRunnerVo.getStatus().split(",")).collect(toList());
            String finalStatus;
            if (statusList.contains(JobNodeStatus.SUCCEED.getValue())) {
                finalStatus = JobPhaseStatus.COMPLETED.getValue();
            } else if (statusList.contains(JobNodeStatus.WAIT_INPUT.getValue())) {
                finalStatus = JobNodeStatus.WAIT_INPUT.getValue();
            } else if (statusList.contains(JobNodeStatus.RUNNING.getValue())) {
                finalStatus = JobNodeStatus.RUNNING.getValue();
            } else if (statusList.contains(JobNodeStatus.FAILED.getValue())) {
                finalStatus = JobNodeStatus.FAILED.getValue();
            } else if (statusList.contains(JobNodeStatus.ABORTED.getValue())) {
                finalStatus = JobNodeStatus.ABORTED.getValue();
            } else if (statusList.contains(JobNodeStatus.PAUSED.getValue())) {
                finalStatus = JobNodeStatus.PAUSED.getValue();
            } else if (statusList.contains(JobNodeStatus.WAITING.getValue())) {
                finalStatus = JobNodeStatus.WAITING.getValue();
            } else {
                finalStatus = JobNodeStatus.PENDING.getValue();
            }
            autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(currentPhase.getId()), jobPhaseRunnerVo.getRunnerMapId(), finalStatus);
        }
    }


    /**
     * 重置autoexec 作业节点状态
     *
     * @param jobVo 作业
     */
    @Override
    public void resetRunnerJobNodeStatus(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo phaseVo = jobVo.getExecutePhase();
        //重置mongodb node 状态
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(jobVo.getExecuteJobNodeVoList())) {
            for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getExecuteJobNodeVoList()) {
                runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
                updatePartialNodeJobAndPhaseWithRunnerId(jobVo.getExecutePhase(), nodeVo.getRunnerMapId(), jobVo, null, null);
                List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(phaseVo.getJobId(), Collections.singletonList(phaseVo.getId()));
                List<String> statusList = jobPhaseRunnerVos.stream().map(AutoexecJobPhaseRunnerVo::getStatus).collect(toList());
                String finalJobPhaseStatus = getJobPhaseStatus(statusList, null);
                autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(phaseVo.getId(), finalJobPhaseStatus, null, phaseVo.getStartTime()));
            }
            runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        } else {
            //重置所有节点状态
            runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), Collections.singletonList(jobVo.getExecutePhase().getId()));
        }

        //计算最终的作业状态
        String jobStatus = getJobStatus(jobVo.getId(), null);
        jobVo.setStatus(jobStatus);
        autoexecJobMapper.updateJobStatus(jobVo);

        checkRunnerHealth(runnerVos);
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getExecutePhase();
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        paramJson.put("phaseName", currentPhaseVo.getName());
        paramJson.put("execMode", currentPhaseVo.getExecMode());
        paramJson.put("phaseNodeList", jobVo.getExecuteJobNodeVoList());
        paramJson.put("jobPhaseNodeSqlList", jobVo.getJobPhaseNodeSqlList());
        for (RunnerMapVo runner : runnerVos) {
            autoexecJobMapper.updateJobPhaseRunnerStatusByPhaseIdAndRunnerIdAndStatus(currentPhaseVo.getId(), runner.getRunnerMapId(), JobPhaseStatus.PENDING.getValue());
            String url = runner.getUrl() + "api/rest/job/phase/node/status/reset";
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
            JSONObject resultJson = requestUtil.getResultJson();
            if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
        }
    }


    @Override
    public void updateJobNodeStatus(List<RunnerMapVo> runnerVos, AutoexecJobVo jobVo, String nodeStatus) {
        AutoexecJobPhaseVo currentPhase = jobVo.getExecutePhase();
        String jobPhaseStatus = getJobPhaseStatus(currentPhase);
        currentPhase.setStatus(jobPhaseStatus);
        autoexecJobMapper.updateJobPhaseStatus(currentPhase);
        String jobStatus = getJobStatus(jobVo.getId(), null);
        jobVo.setStatus(jobStatus);
        autoexecJobMapper.updateJobStatus(jobVo);
        checkRunnerHealth(runnerVos);
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        paramJson.put("phaseName", jobVo.getExecutePhase().getName());
        paramJson.put("execMode", jobVo.getExecutePhase().getExecMode());
        paramJson.put("phaseNodeList", jobVo.getExecuteJobNodeVoList());
        paramJson.put("nodeStatus", nodeStatus);
        for (RunnerMapVo runner : runnerVos) {
            String url = runner.getUrl() + "api/rest/job/phase/node/status/update";
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
            JSONObject resultJson = requestUtil.getResultJson();
            if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
        }
    }

    /**
     * 检查runner联通性
     */
    @Override
    public void checkRunnerHealth(List<? extends RunnerMapVo> runnerVos) {

        String url;
        for (RunnerMapVo runner : runnerVos) {
            if (runner.getRunnerMapId() == null) {
                throw new RunnerMapNotMatchRunnerException(runner.getRunnerMapId());
            }
            if (StringUtils.isBlank(runner.getUrl())) {
                throw new AutoexecJobRunnerNotFoundException(runner.getRunnerMapId());
            }
            url = runner.getUrl() + "api/rest/health/check";
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(new JSONObject().toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
            if (requestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(requestUtil.getError())) {
                Long statusLcd = System.currentTimeMillis();
                runnerMapper.updateStatusById(runner.getId(), RunnerStatus.DISCONNECTED.getValue(), new Date(statusLcd));
                throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s", url, requestUtil.getResult(), requestUtil.getResponseCode(), requestUtil.getErrorMsg(), requestUtil.getError()));
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
        if (jobVo.getExecuteJobGroupVo() == null || jobVo.getExecuteJobGroupVo().getId() == null) {
            throw new AutoexecCombopPhaseGroupIdIsNullException();
        }
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
        if (Objects.equals(jobVo.getExecutePhase().getExecMode(), ExecMode.SQL.getValue())) {
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
            throw new AutoexecJobTargetOrRunnerNotFoundException();
        }
        autoexecJobMapper.updateJobStatus(jobVo);
        Integer isFirstFire = Arrays.asList(JobAction.FIRE.getValue(), JobAction.RESET_REFIRE.getValue()).contains(jobVo.getAction()) ? 1 : 0;
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("isNoFireNext", jobVo.getIsNoFireNext());
        paramJson.put("isFirstFire", isFirstFire);
        JSONObject passThroughEnv = jobVo.getPassThroughEnv();
        if (jobVo.getExecuteJobGroupVo() == null) {
            throw new AutoexecJobGroupNotFoundException(jobVo.getId());
        }
        if (jobVo.getExecutePhase() != null) {
            paramJson.put("jobPhaseNameList", Collections.singletonList(jobVo.getExecutePhase().getName()));
        }

        paramJson.put("jobGroupSortList", Collections.singletonList(jobVo.getExecuteJobGroupVo().getSort()));

        if (jobVo.getExecutePhase() != null && Objects.equals(jobVo.getExecutePhase().getExecMode(), ExecMode.SQL.getValue())) {
            paramJson.put("jobPhaseNodeSqlList", jobVo.getJobPhaseNodeSqlList());
            if (CollectionUtils.isNotEmpty(jobVo.getJobPhaseNodeSqlList())) {
                passThroughEnv.put("isPartialNodeOrSqlRun", 1);
            }
        } else {
            paramJson.put("jobPhaseResourceIdList", jobVo.getExecuteResourceIdList());
            if (CollectionUtils.isNotEmpty(jobVo.getExecuteResourceIdList())) {
                passThroughEnv.put("isPartialNodeOrSqlRun", 1);
            }
        }
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        Long execid = SnowflakeUtil.uniqueLong();
        passThroughEnv.put("groupSort", jobVo.getExecuteJobGroupVo().getSort());
        if (jobVo.getExecutePhase() != null) {
            passThroughEnv.put("phaseSort", jobVo.getExecutePhase().getSort());
        }
        passThroughEnv.put("isFirstFire", isFirstFire);
        passThroughEnv.put("EXECUSER_TOKEN", userMapper.getUserTokenByUser(UserContext.get().getUserId()));
        passThroughEnv.put("EXECUSER_UUID", UserContext.get().getUserUuid());
        for (RunnerMapVo runner : runnerVos) {
            jobVo.getEnvironment().put("RUNNER_ID", runner.getRunnerMapId());
            String url = runner.getUrl() + "api/rest/job/exec";
            passThroughEnv.put("runnerId", runner.getRunnerMapId());
            paramJson.put("passThroughEnv", passThroughEnv);
            paramJson.put("environment", jobVo.getEnvironment());
            paramJson.put("execid", String.valueOf(execid));
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
            if (httpRequestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(httpRequestUtil.getError())) {
                throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s", url, httpRequestUtil.getResult(), httpRequestUtil.getResponseCode(), httpRequestUtil.getErrorMsg(), httpRequestUtil.getError()));
            }
            AutoexecJobExecVo execVo = new AutoexecJobExecVo(jobVo.getId(), runner.getRunnerMapId(), execid, jobVo.getAction());
            autoexecJobMapper.insertJobExec(execVo);
        }

    }

    @Override
    public void fireOrResetRefireWaiting(AutoexecJobVo jobVo) {
        jobVo.setStatus(JobStatus.WAITING.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
        //找到第一个组的第一个phase状态更新成排队中
        AutoexecJobPhaseVo firstPhase = autoexecJobMapper.getJobFirstPhaseByGroupId(jobVo.getExecuteJobGroupVo().getId());
        autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(Collections.singletonList(firstPhase.getId()), JobPhaseStatus.WAITING.getValue());
        List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobVo.getId(), Collections.singletonList(firstPhase.getId()));
        for (AutoexecJobPhaseRunnerVo jobPhaseRunnerVo : jobPhaseRunnerVos) {
            autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(firstPhase.getId()), jobPhaseRunnerVo.getRunnerMapId(), JobPhaseStatus.WAITING.getValue());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePhaseJobStatus2Failed(AutoexecJobVo jobVo, AutoexecJobPhaseVo jobPhaseVo) {
        jobPhaseVo.setStatus(JobStatus.FAILED.getValue());
        autoexecJobMapper.updateJobPhaseStatus(jobPhaseVo);
        jobVo.setStatus(JobStatus.FAILED.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
    }

    @Override
    public void getAllSubJobList(Long jobId, List<AutoexecJobVo> subJobList) {
        List<AutoexecJobVo> subjobVoList = autoexecJobMapper.getJobListLockByParentId(jobId);
        if (CollectionUtils.isNotEmpty(subjobVoList)) {
            for (AutoexecJobVo subJobVo : subjobVoList) {
                if (subJobList.stream().noneMatch(o -> Objects.equals(o.getId(), subJobVo.getId()))) {
                    subJobList.add(subJobVo);
                }
                getAllSubJobList(subJobVo.getId(), subJobList);
            }
        }
    }

    @Override
    public void batchExecuteJobAction(AutoexecJobVo jobVo, JobAction jobAction) throws Exception {
        List<AutoexecJobVo> autoexecJobVos = com.google.common.collect.Lists.newArrayList(Collections.singletonList(jobVo));
        getAllSubJobList(jobVo.getId(), autoexecJobVos);
        for (AutoexecJobVo job : autoexecJobVos) {
            job.setAction(jobVo.getAction());
            job.setIsTakeOver(jobVo.getIsTakeOver());
            IAutoexecJobActionHandler refireAction = AutoexecJobActionHandlerFactory.getAction(jobAction.getValue());
            refireAction.doService(job);
        }
    }

    /**
     * 根据作业id获取作业等待详情
     *
     * @param jobId 作业id
     */
    @Override
    public JSONArray getAutoexecJobWaitingDetail(Long jobId) {
        JSONArray queueStatusArray = new JSONArray();
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobRunnerMapByJobId(jobId);

        JSONObject params = new JSONObject();
        params.put("jobId", jobId);
        checkRunnerHealth(runnerVos);
        for (RunnerMapVo runner : runnerVos) {
            String url = runner.getUrl() + "api/rest/job/waiting/detail/get";
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(params.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
            if (requestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(requestUtil.getError())) {
                throw new RunnerHttpRequestException("Request failed! " + url + ":" + requestUtil.getError());
            }
            JSONObject resultJson = requestUtil.getResultJson();
            JSONObject queueJson = resultJson.getJSONObject("Return");
            if (MapUtils.isNotEmpty(queueJson)) {
                for (Map.Entry<String, Object> entry : queueJson.entrySet()) {
                    String key = entry.getKey();
                    if (Objects.equals(key, "count")) {
                        continue;
                    }
                    JSONObject value = JSON.parseObject(entry.getValue().toString());
                    JSONObject queueStatus = new JSONObject();
                    queueStatus.put("sort", key + "/" + queueJson.getString("count"));
                    queueStatus.put("command", value.getString("command"));
                    queueStatus.put("fcd", TimeUtil.convertDateToString(new Date(value.getLong("fcd")), TimeUtil.YYYY_MM_DD_HH_MM_SS));
                    queueStatus.put("runner", runner.getName() + ":" + runner.getPort());
                    queueStatus.put("runnerId", runner.getId());
                    queueStatus.put("groupSortList", value.getJSONArray("groupSortList"));
                    queueStatusArray.add(queueStatus);
                }
            }
        }
        return queueStatusArray;
    }

    @Override
    public void abortOrPause(AutoexecJobVo jobVo, String action, String statusIng) {
        //如果作业本身是已完成 则无需中止
        if (Arrays.asList(JobStatus.COMPLETED.getValue(), JobStatus.ABORTED.getValue(), JobStatus.PAUSED.getValue(), JobStatus.FAILED.getValue(), JobStatus.REVOKED.getValue()).contains(jobVo.getStatus())) {
            return;
        }
        //如果不是暂停动作且作业状态是waitInput,则更新job状态 为中止中
        if (!(Objects.equals(JobAction.PAUSE.getValue(), action) && Objects.equals(jobVo.getStatus(), JobPhaseStatus.WAIT_INPUT.getValue()))) {
            jobVo.setStatus(statusIng);
            autoexecJobMapper.updateJobStatus(jobVo);
        }
        //更新phase状态 为中止中
        jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobVo.getId()));
        List<AutoexecJobPhaseRunnerVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobId(jobVo.getId());
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            //如果是waitInput则只允许中止修改作业阶段以及对应runner状态
            if (Arrays.asList(JobPhaseStatus.RUNNING.getValue(), JobPhaseStatus.WAITING.getValue()).contains(jobPhase.getStatus()) || (Objects.equals(JobAction.ABORT.getValue(), action) && Objects.equals(jobPhase.getStatus(), JobPhaseStatus.WAIT_INPUT.getValue()))) {
                jobPhase.setStatus(statusIng);
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
                for (AutoexecJobPhaseRunnerVo jobPhaseRunnerVo : runnerVos) {
                    if (Objects.equals(jobPhase.getId(), jobPhaseRunnerVo.getJobPhaseId()) && Arrays.asList(JobPhaseStatus.RUNNING.getValue(), JobPhaseStatus.WAITING.getValue()).contains(jobPhaseRunnerVo.getStatus()) || (Objects.equals(JobAction.ABORT.getValue(), action) && Objects.equals(jobPhaseRunnerVo.getStatus(), JobPhaseStatus.WAIT_INPUT.getValue()))) {
                        autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(jobPhase.getId()), jobPhaseRunnerVo.getRunnerMapId(), statusIng);
                    }
                }
            }
        }
        //更新node状态 为中止中
//        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndNodeStatusList(jobVo.getId(), Arrays.asList(JobPhaseStatus.WAITING.getValue(), JobNodeStatus.RUNNING.getValue()));
//        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
//            nodeVo.setStatus(statusIng);
//            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
//        }


        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(AutoexecJobPhaseRunnerVo::getUrl))), ArrayList::new));
        checkRunnerHealth(runnerVos);
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        String result = StringUtils.EMPTY;
        String url = null;
        try {
            for (RunnerMapVo runner : runnerVos) {
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getRunnerMapId());
                    put("EXECUSER_UUID", UserContext.get().getUserUuid(true));
                }});
                url = runner.getUrl() + "api/rest/job/" + action;
                HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
                if (StringUtils.isNotBlank(requestUtil.getError())) {
                    throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
                }
                JSONObject resultJson = requestUtil.getResultJson();
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RunnerConnectRefusedException(url + " " + result);
        }
    }

    @Override
    public void cleanHistoryJobAutoexecData(List<Long> runnerMapIdList, int dayBefore) throws Exception {
        List<RunnerMapVo> runnerVos = runnerMapper.getRunnerByRunnerMapIdList(runnerMapIdList);
        if (CollectionUtils.isNotEmpty(runnerVos)) {
            runnerVos = runnerVos.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getId))), ArrayList::new));
            UserContext.init(neatlogic.framework.common.constvalue.systemuser.SystemUser.SYSTEM);
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
            for (RunnerMapVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/data/purge";
                JSONObject paramJson = new JSONObject();
                paramJson.put("expiredDays", dayBefore);
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getRunnerMapId());
                }});
                HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setAuthType(AuthenticateType.BUILDIN).setPayload(paramJson.toJSONString()).setConnectTimeout(AutoexecConfig.RUNNER_CONNECT_TIMEOUT()).sendRequest();
                if (StringUtils.isNotBlank(requestUtil.getError())) {
                    logger.error(requestUtil.getError());
                    throw new AutoexecJobDeleteException(runner);
                }
            }
        }
    }

    @Override
    public String getJobStatus(Long jobId, String runnerStatus) {
        String jobStatus = null;
        Long execJobId = autoexecJobMapper.getJobRunnerExec(jobId);
        if (Objects.equals(runnerStatus, JobStatus.WAIT_INPUT.getValue())) {
            jobStatus = JobStatus.WAIT_INPUT.getValue();
        } else {
            List<AutoexecJobPhaseVo> phaseList = autoexecJobMapper.getJobPhaseListByJobId(jobId);
            Set<String> statusSet = phaseList.stream().map(AutoexecJobPhaseVo::getStatus).collect(Collectors.toSet());

            int pendingCount = 0;
            int runningCount = 0;
            int waitInputCount = 0;
            int failedCount = 0;
            int abortingCount = 0;
            int abortedCount = 0;
            int completedCount = 0;
            int ignoredCount = 0;
            int pausingCount = 0;
            int pausedCount = 0;
            int waitingCount = 0;

            //jobRunnerPhaseStatusSet: 当前runner对应的所有phase的状态集合
            for (String phaseStatus : statusSet) {
                if (Objects.equals(phaseStatus, JobStatus.WAIT_INPUT.getValue())) {
                    waitInputCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.RUNNING.getValue())) {
                    runningCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.WAITING.getValue())) {
                    waitingCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.PENDING.getValue())) {
                    pendingCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.ABORTING.getValue())) {
                    abortingCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.ABORTED.getValue())) {
                    abortedCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.PAUSING.getValue())) {
                    pausingCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.PAUSED.getValue())) {
                    pausedCount++;
                } else if (Objects.equals(phaseStatus, JobStatus.FAILED.getValue())) {
                    failedCount++;
                } else {
                    completedCount++;
                }
            }
            if (Objects.equals(runnerStatus, JobStatus.RUNNING.getValue())) {
                //如果更新作业状态接口调用过来的状态是running，这是，phase和node都还没有进入running，需要额外优先处理
                if (waitInputCount > 0) {
                    //如存在waitInput则状态设置为waitInput
                    jobStatus = JobStatus.WAIT_INPUT.getValue();
                } else {
                    //否则是running
                    jobStatus = JobStatus.RUNNING.getValue();
                }
            } else {
                if (waitInputCount > 0) {
                    jobStatus = JobStatus.WAIT_INPUT.getValue();
                } else if (runningCount > 0) {
                    jobStatus = JobStatus.RUNNING.getValue();
                } else if (execJobId != null) {
                    //如果进程还在则是running
                    jobStatus = JobStatus.RUNNING.getValue();
                } else if (abortingCount > 0) {
                    jobStatus = JobStatus.ABORTING.getValue();
                } else if (pausingCount > 0) {
                    jobStatus = JobStatus.PAUSING.getValue();
                } else if (abortedCount > 0) {
                    jobStatus = JobStatus.ABORTED.getValue();
                } else if (failedCount > 0) {
                    jobStatus = JobStatus.FAILED.getValue();
                } else if (pausedCount > 0) {
                    jobStatus = JobStatus.PAUSED.getValue();
                } else if (completedCount > 0) {
                    if (pendingCount == 0) {
                        //没有pending和其它状态的phase，全部都是complete
                        jobStatus = JobStatus.COMPLETED.getValue();
                    } else {
                        //如果存在pending而且阶段状态是complete，修正为PAUSED状态
                        jobStatus = JobStatus.PAUSED.getValue();
                    }
                } else if (waitingCount > 0) {
                    //降低waiting优先级。防止中止第一个round的runner，第二round是另外一个runner还是waiting，导致阶段状态计算还是waiting
                    jobStatus = JobStatus.WAITING.getValue();
                } else {
                    //没有其它状态，全部是pending
                    jobStatus = JobStatus.PENDING.getValue();
                }
            }
        }

        return jobStatus;
    }

    @Override
    public String getJobPhaseStatus(List<String> statusList, String currentPhaseStatus) {
        int pendingCount = 0;
        int runningCount = 0;
        int waitInputCount = 0;
        int failedCount = 0;
        int abortingCount = 0;
        int abortedCount = 0;
        int completeCount = 0;
        int ignoredCount = 0;
        int pausingCount = 0;
        int pausedCount = 0;
        int waitingCount = 0;

        Set<String> statusSet = new HashSet<>(statusList);
        for (String phaseStatus : statusSet) {

            if (Objects.equals(phaseStatus, JobPhaseStatus.WAIT_INPUT.getValue())) {
                waitInputCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.RUNNING.getValue())) {
                runningCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.WAITING.getValue())) {
                waitingCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.PENDING.getValue())) {
                pendingCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.ABORTING.getValue())) {
                abortingCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.ABORTED.getValue())) {
                abortedCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.PAUSING.getValue())) {
                pausingCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.PAUSED.getValue())) {
                pausedCount++;
            } else if (Objects.equals(phaseStatus, JobPhaseStatus.FAILED.getValue())) {
                failedCount++;
            } else {
                completeCount++;
            }
        }
        //System.out.println(String.join(",", statusSet));
        String finalPhaseStatus = null;
        if (Objects.equals(currentPhaseStatus, JobPhaseStatus.RUNNING.getValue())) {
            //如果phase给出的状态是running，这个时候节点状态还没有更新
            //如果存在waitInput，则状态为waitInput，否则直接是running
            if (waitInputCount > 0) {
                finalPhaseStatus = JobPhaseStatus.WAIT_INPUT.getValue();
            } else {
                finalPhaseStatus = JobPhaseStatus.RUNNING.getValue();
            }
        } else {
            if (waitInputCount > 0) {
                finalPhaseStatus = JobPhaseStatus.WAIT_INPUT.getValue();
            } else if (runningCount > 0) {
                finalPhaseStatus = JobPhaseStatus.RUNNING.getValue();
            } else if (abortingCount > 0) {
                finalPhaseStatus = JobPhaseStatus.ABORTING.getValue();
            } else if (pausingCount > 0) {
                finalPhaseStatus = JobPhaseStatus.PAUSING.getValue();
            } else if (abortedCount > 0) {
                finalPhaseStatus = JobPhaseStatus.ABORTED.getValue();
            } else if (failedCount > 0) {
                finalPhaseStatus = JobPhaseStatus.FAILED.getValue();
            } else if (pausedCount > 0) {
                finalPhaseStatus = JobPhaseStatus.PAUSED.getValue();
            } else if (completeCount > 0) {
                if (pendingCount == 0) {
                    //如果没有pending和，全部都是complete
                    finalPhaseStatus = JobPhaseStatus.COMPLETED.getValue();
                } else {
                    //如果存在pending而且阶段状态是complete，修正为PAUSED状态
                    finalPhaseStatus = JobPhaseStatus.PAUSED.getValue();
                }
            } else if (waitingCount > 0) {
                //降低waiting优先级。防止中止第一个round的runner，第二round是另外一个runner还是waiting，导致阶段状态计算还是waiting
                finalPhaseStatus = JobPhaseStatus.WAITING.getValue();
            } else {
                //其它状态都没有，只有pending
                finalPhaseStatus = JobPhaseStatus.PENDING.getValue();
            }
        }
        //System.out.println("finalPhaseStatus:" + finalPhaseStatus);
        return finalPhaseStatus;
    }

    private String getJobPhaseStatus(AutoexecJobPhaseVo jobPhaseVo) {
        List<AutoexecJobPhaseRunnerVo> jobPhaseRunnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobPhaseVo.getJobId(), Collections.singletonList(jobPhaseVo.getId()));
        List<String> statusList = jobPhaseRunnerVos.stream().map(AutoexecJobPhaseRunnerVo::getStatus).collect(toList());
        return getJobPhaseStatus(statusList, null);
    }

    /**
     * 刷新作业runner状态
     *
     * @param jobId     作业id
     * @param updateTag 更新标记
     */
    private void refreshJobRunner(Long jobId, Long updateTag) {
        //保存作业执行器状态
        List<AutoexecJobPhaseRunnerVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobId(jobId);
        if (CollectionUtils.isNotEmpty(runnerVos)) {
            runnerVos = runnerVos.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(AutoexecJobPhaseRunnerVo::getRunnerMapId))), ArrayList::new));
            autoexecJobMapper.insertJobRunner(runnerVos, updateTag);
            if (updateTag != null) {
                autoexecJobMapper.deleteJobByJobIdAndUpdateTag(jobId, updateTag);
            }
        }
    }

    @Override
    public String updatePartialNodeJobAndPhaseWithRunnerId(AutoexecJobPhaseVo jobPhaseVo, Long runnerId, AutoexecJobVo jobVo, String currentPhaseStatus, Integer phaseRunnerWarnCount) {
        List<String> statusList;
        //如果单个重跑则需要根据节点纠正当前阶段runner状态
        List<String> needCountStatusList = Arrays.stream(JobNodeStatus.values()).map(JobNodeStatus::getValue).filter(value -> !Objects.equals(value, JobNodeStatus.INVALID.getValue())).collect(toList());
        if (Objects.equals(jobPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            statusList = autoexecJobSourceActionHandler.getPhaseSqlStatusList(jobPhaseVo, runnerId, needCountStatusList);
        } else {
            statusList = autoexecJobMapper.getJobPhaseNodeStatusList(jobPhaseVo.getJobId(), jobPhaseVo.getId(), runnerId, needCountStatusList);
        }
        currentPhaseStatus = getJobPhaseStatus(statusList, currentPhaseStatus);
        autoexecJobMapper.updateJobPhaseRunnerStatusAndWarnCount(jobPhaseVo.getId(), runnerId, currentPhaseStatus, phaseRunnerWarnCount);
        return currentPhaseStatus;
    }
}
