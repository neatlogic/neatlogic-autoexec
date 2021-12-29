/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceListApiCrossoverService;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.util.IpUtil;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.dto.runner.GroupNetworkVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @since 2021/4/12 18:44
 **/
@Service
public class AutoexecJobServiceImpl implements AutoexecJobService {
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
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    RunnerMapper runnerMapper;
    @Resource
    ResourceCenterMapper resourceCenterMapper;

    @Override
    public AutoexecJobVo saveAutoexecCombopJob(AutoexecCombopVo combopVo, AutoexecJobInvokeVo invokeVo, Integer threadCount, JSONObject paramJson) {
        AutoexecCombopConfigVo config = combopVo.getConfig();
        if (combopVo.getIsTest() == null || !combopVo.getIsTest()) {
            combopVo.setOperationType(CombopOperationType.COMBOP.getValue());
            combopVo.setRuntimeParamList(autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopVo.getId()));
        }
        AutoexecJobVo jobVo = new AutoexecJobVo(combopVo, invokeVo.getSource(), threadCount, paramJson);
        //更新关联来源关系
        invokeVo.setJobId(jobVo.getId());
        if (!Objects.equals(JobSource.HUMAN.getValue(), invokeVo.getSource())) {
            autoexecJobMapper.insertIgnoreIntoJobInvoke(invokeVo);
        }
        //保存作业基本信息
        autoexecJobMapper.insertJob(jobVo);
        autoexecJobMapper.insertIgnoreJobParamContent(new AutoexecJobParamContentVo(jobVo.getParamHash(), jobVo.getParamStr()));
        //保存作业执行目标
        AutoexecCombopExecuteConfigVo combopExecuteConfigVo = config.getExecuteConfig();
        String userName = StringUtils.EMPTY;
        Long protocolId = null;
        if (combopExecuteConfigVo != null) {
            //先获取组合工具配置的执行用户和协议
            userName = combopExecuteConfigVo.getExecuteUser();
            protocolId = combopExecuteConfigVo.getProtocolId();
        }
        //保存阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = new ArrayList<>();
        jobVo.setPhaseList(jobPhaseVoList);
        //创建作业当前phase为sort为0
        jobVo.setCurrentPhaseSort(0);
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            AutoexecJobPhaseVo jobPhaseVo = new AutoexecJobPhaseVo(autoexecCombopPhaseVo, jobVo.getId());
            autoexecJobMapper.insertJobPhase(jobPhaseVo);
            if (jobPhaseVo.getSort() == 0) {//只需要第一个剧本，供后续激活执行
                jobPhaseVoList.add(jobPhaseVo);
            }
            AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo = autoexecCombopPhaseVo.getConfig();
            //jobPhaseNode
            //如果是target 则获取执行目标，否则随机分配runner
            if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue(), ExecMode.SQL.getValue()).contains(autoexecCombopPhaseVo.getExecMode())) {
                initPhaseExecuteUserAndProtocolAndNode(userName, protocolId, jobVo, jobPhaseVo, combopExecuteConfigVo, combopPhaseExecuteConfigVo);
            } else {
                List<RunnerMapVo> runnerMapList = runnerMapper.getAllRunnerMap();
                //TODO 负载均衡
                if (CollectionUtils.isEmpty(runnerMapList)) {
                    throw new AutoexecJobRunnerNotMatchException();
                }
                int runnerMapIndex = (int) (Math.random() * runnerMapList.size());
                RunnerMapVo runnerMapVo = runnerMapList.get(runnerMapIndex);
                Date nowTime = new Date(System.currentTimeMillis());
                jobPhaseVo.setLcd(nowTime);
                AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), jobPhaseVo, "runner", JobNodeStatus.PENDING.getValue(), userName, protocolId);
                autoexecJobMapper.insertJobPhaseNode(nodeVo);
                nodeVo.setRunnerMapId(runnerMapVo.getRunnerMapId());
                autoexecJobMapper.insertIgnoreJobPhaseNodeRunner(new AutoexecJobPhaseNodeRunnerVo(nodeVo));
                autoexecJobMapper.insertDuplicateJobPhaseRunner(nodeVo);
            }
            //jobPhaseOperation
            List<AutoexecJobPhaseOperationVo> jobPhaseOperationVoList = new ArrayList<>();
            jobPhaseVo.setOperationList(jobPhaseOperationVoList);
            List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = combopPhaseExecuteConfigVo.getPhaseOperationList();
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : combopPhaseOperationList) {
                String operationType = autoexecCombopPhaseOperationVo.getOperationType();
                Long operationId = autoexecCombopPhaseOperationVo.getOperationId();
                AutoexecJobPhaseOperationVo jobPhaseOperationVo = null;
                if (CombopOperationType.SCRIPT.getValue().equalsIgnoreCase(operationType)) {
                    AutoexecScriptVo scriptVo = null;
                    AutoexecScriptVersionVo scriptVersionVo = null;
                    String script = StringUtils.EMPTY;
                    if (combopVo.getIsTest() != null && combopVo.getIsTest()) {
                        scriptVersionVo = autoexecScriptMapper.getVersionByVersionId(operationId);
                        if (scriptVersionVo == null) {
                            throw new AutoexecScriptVersionNotFoundException(operationId);
                        }
                        scriptVo = autoexecScriptMapper.getScriptBaseInfoById(scriptVersionVo.getScriptId());
                        script = autoexecCombopService.getOperationActiveVersionScriptByOperation(scriptVersionVo);
                    } else {
                        scriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
                        scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
                        script = autoexecCombopService.getOperationActiveVersionScriptByOperationId(operationId);
                    }
                    jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, scriptVo, scriptVersionVo, script, jobPhaseVoList);
                } else if (CombopOperationType.TOOL.getValue().equalsIgnoreCase(operationType)) {
                    AutoexecToolVo toolVo = autoexecToolMapper.getToolById(operationId);
                    jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, toolVo, jobPhaseVoList);
                }
                autoexecJobMapper.insertJobPhaseOperation(jobPhaseOperationVo);
                assert jobPhaseOperationVo != null;
                autoexecJobMapper.insertIgnoreJobParamContent(new AutoexecJobParamContentVo(jobPhaseOperationVo.getParamHash(), jobPhaseOperationVo.getParamStr()));
                jobPhaseOperationVoList.add(jobPhaseOperationVo);
            }
        }
        return jobVo;
    }

    /**
     * 如果ExecMode 不是 local, 则初始化获取执行用户、协议和执行目标
     *
     * @param userName                   作业设置的执行用户
     * @param protocolId                 作业设置的执行协议Id
     * @param jobVo                      作业
     * @param jobPhaseVo                 作业阶段
     * @param combopExecuteConfigVo      作业设置的节点配置
     * @param combopPhaseExecuteConfigVo 阶段设置的节点配置
     */
    private void initPhaseExecuteUserAndProtocolAndNode(String userName, Long protocolId, AutoexecJobVo jobVo, AutoexecJobPhaseVo jobPhaseVo, AutoexecCombopExecuteConfigVo combopExecuteConfigVo, AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo) {
        AutoexecCombopExecuteConfigVo executeConfigVo = combopPhaseExecuteConfigVo.getExecuteConfig();
        boolean isHasNode = false;
        boolean isPhaseConfig = false;
        if (executeConfigVo != null) {
            if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                userName = executeConfigVo.getExecuteUser();
            }
            if (executeConfigVo.getProtocolId() != null) {
                protocolId = executeConfigVo.getProtocolId();
            }
            //判断阶段执行节点是否配置
            isPhaseConfig = executeConfigVo.getExecuteNodeConfig() != null
                    && (CollectionUtils.isNotEmpty(executeConfigVo.getExecuteNodeConfig().getTagList())
                    || CollectionUtils.isNotEmpty(executeConfigVo.getExecuteNodeConfig().getSelectNodeList())
                    || CollectionUtils.isNotEmpty(executeConfigVo.getExecuteNodeConfig().getInputNodeList())
                    || CollectionUtils.isNotEmpty(executeConfigVo.getExecuteNodeConfig().getParamList())
            );
            if (isPhaseConfig) {
                isHasNode = getJobNodeList(executeConfigVo, jobVo, jobPhaseVo, userName, protocolId, true);
            }
        }
        //如果阶段没有设置执行目标，则使用全局执行目标
        if (!isPhaseConfig) {
            isHasNode = getJobNodeList(combopExecuteConfigVo, jobVo, jobPhaseVo, userName, protocolId, false);
        }

        //如果都找不到执行节点
        if (!isHasNode) {
            throw new AutoexecJobPhaseNodeNotFoundException(jobPhaseVo.getName(), isPhaseConfig);
        }

    }

    @Override
    public void refreshJobParam(Long jobId, JSONObject paramJson) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (!Objects.equals(CombopOperationType.COMBOP.getValue(), jobVo.getOperationType())) {
            throw new AutoexecJobPhaseOperationMustBeCombopException();
        }
        //补充运行参数真实的值
        List<AutoexecCombopParamVo> paramList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(jobVo.getOperationId());
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
        jobVo.setParamStr(combopParamsResult.toJSONString());
        autoexecJobMapper.insertIgnoreJobParamContent(new AutoexecJobParamContentVo(jobVo.getParamHash(), jobVo.getParamStr()));
        autoexecJobMapper.updateJobParamHashById(jobVo.getId(), jobVo.getParamHash());
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, int sort, JSONObject executeConfig) {
        AutoexecCombopExecuteConfigVo combopExecuteConfigVo = null;
        //优先使用传进来的执行节点
        if (MapUtils.isNotEmpty(executeConfig)) {
            combopExecuteConfigVo = JSON.toJavaObject(executeConfig, AutoexecCombopExecuteConfigVo.class);
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        AutoexecCombopConfigVo configVo = JSON.toJavaObject(jobVo.getConfig(), AutoexecCombopConfigVo.class);
        //获取组合工具执行目标 执行用户和协议
        //非空场景，用于重跑替换执行配置（执行目标，用户，协议）
        if (combopExecuteConfigVo == null) {
            combopExecuteConfigVo = configVo.getExecuteConfig();
        }
        String userName = StringUtils.EMPTY;
        Long protocolId = null;
        if (combopExecuteConfigVo != null) {
            userName = combopExecuteConfigVo.getExecuteUser();
            protocolId = combopExecuteConfigVo.getProtocolId();
        }
        //获取当前所有target阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndSort(jobId, sort);
        //只刷新当前target|sql阶段
        List<AutoexecCombopPhaseVo> combopPhaseList = configVo.getCombopPhaseList().stream().filter(o -> o.getSort() == sort && Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.SQL.getValue()).contains(o.getExecMode())).collect(Collectors.toList());
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            AutoexecCombopPhaseConfigVo combopPhaseExecuteConfigVo = autoexecCombopPhaseVo.getConfig();
            Optional<AutoexecJobPhaseVo> jobPhaseVoOptional = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getName(), autoexecCombopPhaseVo.getName())).findFirst();
            if (jobPhaseVoOptional.isPresent()) {
                AutoexecJobPhaseVo jobPhaseVo = jobPhaseVoOptional.get();
                jobPhaseVo.setCombopId(jobVo.getOperationId());
                initPhaseExecuteUserAndProtocolAndNode(userName, protocolId, jobVo, jobPhaseVo, combopExecuteConfigVo, combopPhaseExecuteConfigVo);
            }
        }
    }

    @Override
    public void refreshJobNodeList(Long jobId, JSONObject executeConfig) {
        List<AutoexecJobPhaseVo> phaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobId);
        for (AutoexecJobPhaseVo phaseVo : phaseVoList) {
            refreshJobPhaseNodeList(jobId, phaseVo.getSort(), executeConfig);
        }
    }

    @Override
    public void getAutoexecJobDetail(AutoexecJobVo jobVo, Integer sort) {
        AutoexecJobParamContentVo paramContentVo = autoexecJobMapper.getJobParamContent(jobVo.getParamHash());
        if (paramContentVo != null) {
            jobVo.setParamStr(paramContentVo.getContent());
        }
        List<AutoexecJobPhaseVo> phaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId());
        if (sort != null) {
            phaseVoList = phaseVoList.stream().filter(o -> Objects.equals(o.getSort(), sort)).collect(Collectors.toList());
        }
        jobVo.setPhaseList(phaseVoList);
        for (AutoexecJobPhaseVo phaseVo : phaseVoList) {
            List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobVo.getId(), phaseVo.getId());
            phaseVo.setOperationList(operationVoList);
            for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                paramContentVo = autoexecJobMapper.getJobParamContent(operationVo.getParamHash());
                if (paramContentVo != null) {
                    operationVo.setParamStr(paramContentVo.getContent());
                }
            }
        }
    }


    /**
     * 根据目标ip自动匹配runner
     *
     * @param ip 目标ip
     * @return runnerId
     */
    private Long getRunnerByIp(String ip) {
        List<GroupNetworkVo> networkVoList = runnerMapper.getAllNetworkMask();
        for (GroupNetworkVo networkVo : networkVoList) {
            if (IpUtil.isBelongSegment(ip, networkVo.getNetworkIp(), networkVo.getMask())) {
                RunnerGroupVo groupVo = runnerMapper.getRunnerMapGroupById(networkVo.getGroupId());
                if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
                    throw new AutoexecJobRunnerGroupRunnerNotFoundException(groupVo.getName() + "(" + networkVo.getGroupId() + ") ");
                }
                int runnerMapIndex = (int) (Math.random() * groupVo.getRunnerMapList().size());
                RunnerMapVo runnerMapVo = groupVo.getRunnerMapList().get(runnerMapIndex);
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
     * @param nodeConfigVo node配置config
     * @param jobPhaseVo   作业剧本Vo
     * @param userName     连接node 用户
     * @param protocolId   连接node 协议Id
     */
    private boolean getJobNodeList(AutoexecCombopExecuteConfigVo nodeConfigVo, AutoexecJobVo jobVo, AutoexecJobPhaseVo jobPhaseVo, String userName, Long protocolId, boolean isPhaseConfig) {
        if (nodeConfigVo == null) {
            return false;
        }
        boolean isHasNode = false;
        Map<Long, ResourceVo> resourceMap = new HashMap<>();
        List<Long> resourceIdList = new ArrayList<>();
        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = nodeConfigVo.getExecuteNodeConfig();
        if (executeNodeConfigVo == null) {
            return false;
        }
        if (MapUtils.isNotEmpty(executeNodeConfigVo.getFilter())) {
            isHasNode = updateNodeResourceByFilter(executeNodeConfigVo, jobPhaseVo, userName, protocolId);
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList()) || CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            isHasNode = updateNodeResourceByInputAndSelect(executeNodeConfigVo, jobPhaseVo, isPhaseConfig, userName, protocolId);
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getParamList())) {
            isHasNode = updateNodeResourceByParam(jobVo, executeNodeConfigVo, jobPhaseVo, userName, protocolId);
        }

        //删除没有跑过的历史节点 runnerMap
        autoexecJobMapper.deleteJobPhaseNodeRunnerByJobPhaseIdAndLcdAndStatus(jobPhaseVo.getId(), jobPhaseVo.getLcd(), JobNodeStatus.PENDING.getValue());
        //删除没有跑过的历史节点
        autoexecJobMapper.deleteJobPhaseNodeByJobPhaseIdAndLcdAndStatus(jobPhaseVo.getId(), jobPhaseVo.getLcd(), JobNodeStatus.PENDING.getValue());
        //更新该阶段所有不是最近更新的节点为已删除，即非法历史节点
        autoexecJobMapper.updateJobPhaseNodeIsDeleteByJobPhaseIdAndLcd(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        //删除该阶段所有不是最近更新的phase runner
        autoexecJobMapper.deleteJobPhaseRunnerByJobPhaseIdAndLcd(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        return isHasNode;
    }

    /**
     * param
     * 根据运行参数中定义的节点参数 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobPhaseVo          作业阶段
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceByParam(AutoexecJobVo jobVo, AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobPhaseVo jobPhaseVo, String userName, Long protocolId) {
        List<String> paramList = executeNodeConfigVo.getParamList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            JSONArray paramArray = jobVo.getParam();
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
                    List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceFromSoftwareServiceByIdList(new ArrayList<>(resourceIdSet), TenantContext.get().getDataDbName());
                    if (CollectionUtils.isNotEmpty(resourceVoList)) {
                        Date nowTime = new Date(System.currentTimeMillis());
                        jobPhaseVo.setLcd(nowTime);
                        updateJobPhaseNode(jobPhaseVo, resourceVoList, userName, protocolId);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * inputNodeList、selectNodeList
     * 根据输入和选择节点 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobPhaseVo          作业阶段
     * @param isPhaseConfig       是否阶段配置
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceByInputAndSelect(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobPhaseVo jobPhaseVo, boolean isPhaseConfig, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = new ArrayList<>();
        List<ResourceVo> ipPortList = new ArrayList<>();
        List<ResourceVo> lostIpPortList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList())) {
            nodeVoList.addAll(executeNodeConfigVo.getInputNodeList());
        }
        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            nodeVoList.addAll(executeNodeConfigVo.getSelectNodeList());
        }
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            nodeVoList.forEach(o -> {
                ipPortList.add(new ResourceVo(o.getIp(), o.getPort()));
            });
            List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceListByResourceVoList(ipPortList, TenantContext.get().getDataDbName());
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                //根据ip:port去重
                resourceVoList = resourceVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(r -> r.getIp() + ":" + r.getPort()))), ArrayList::new));
                //如果根据ip port 找不到对应的资产，直接返回异常提示
                List<ResourceVo> finalResourceVoList = resourceVoList;
                lostIpPortList = ipPortList.stream().filter(s -> finalResourceVoList.stream().noneMatch(s1 -> Objects.equals(s1.getIp() + s1.getPort(), s.getIp() + s.getPort()))).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(lostIpPortList)) {
                    Date nowTime = new Date(System.currentTimeMillis());
                    jobPhaseVo.setLcd(nowTime);
                    updateJobPhaseNode(jobPhaseVo, resourceVoList, userName, protocolId);
                    return true;
                }
            } else {
                lostIpPortList = ipPortList;
            }
            //无须校验
           /* if (CollectionUtils.isNotEmpty(lostIpPortList)) {
                throw new AutoexecJobPhaseNodeNotFoundException(jobPhaseVo.getName(), lostIpPortList, isPhaseConfig);
            }*/
        }
        return false;
    }

    /**
     * filter
     * 根据过滤器 更新节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobPhaseVo          作业阶段
     * @param userName            执行用户
     * @param protocolId          协议id
     * @throws Exception 异常
     */
    private boolean updateNodeResourceByFilter(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobPhaseVo jobPhaseVo, String userName, Long protocolId) {
        JSONObject filterJson = executeNodeConfigVo.getFilter();
        if (MapUtils.isNotEmpty(filterJson)) {
            JSONObject resourceJson = new JSONObject();
            IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            filterJson.put("pageSize", 100);
            ResourceSearchVo searchVo = resourceCrossoverService.assembleResourceSearchVo(filterJson);
            int count = resourceCenterMapper.getResourceCount(searchVo);
            if (count > 0) {
                Date nowTime = new Date(System.currentTimeMillis());
                jobPhaseVo.setLcd(nowTime);
                int pageCount = PageUtil.getPageCount(count, searchVo.getPageSize());
                for (int i = 1; i <= pageCount; i++) {
                    filterJson.put("currentPage", i);
                    filterJson.put("needPage", true);
                    try {
                        IResourceListApiCrossoverService resourceListApi = CrossoverServiceFactory.getApi(IResourceListApiCrossoverService.class);
                        resourceJson = JSONObject.parseObject(JSONObject.toJSONString(resourceListApi.myDoService(filterJson)));
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        return false;
                    }
                    List<ResourceVo> resourceVoList = JSONObject.parseArray(resourceJson.getString("tbodyList"), ResourceVo.class);
                    if (CollectionUtils.isNotEmpty(resourceVoList)) {
                        updateJobPhaseNode(jobPhaseVo, resourceVoList, userName, protocolId);
                    }
                }
                return true;
            }

        }
        return false;
    }

    private void updateJobPhaseNode(AutoexecJobPhaseVo jobPhaseVo, List<ResourceVo> resourceVoList, String userName, Long protocolId) {
        autoexecJobMapper.updateJobPhaseLcdById(jobPhaseVo.getId(), jobPhaseVo.getLcd());
        resourceVoList.forEach(resourceVo -> {
            AutoexecJobPhaseNodeVo jobPhaseNodeVo = new AutoexecJobPhaseNodeVo(resourceVo, jobPhaseVo.getJobId(), jobPhaseVo, JobNodeStatus.PENDING.getValue(), userName, protocolId);
            jobPhaseNodeVo.setPort(resourceVo.getPort());
            jobPhaseNodeVo.setRunnerMapId(getRunnerByIp(jobPhaseNodeVo.getHost()));
            if (jobPhaseNodeVo.getRunnerMapId() == null) {
                throw new AutoexecJobRunnerNotMatchException(jobPhaseNodeVo.getHost());
            }

            //如果大于 update 0,说明存在旧数据
            Integer result = autoexecJobMapper.updateJobPhaseNodeByJobIdAndPhaseIdAndResourceId(jobPhaseNodeVo);
            if (result == null || result == 0) {
                autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
                //防止旧resource 所以ignore insert
                autoexecJobMapper.insertIgnoreJobPhaseNodeRunner(new AutoexecJobPhaseNodeRunnerVo(jobPhaseNodeVo));
            }
            autoexecJobMapper.insertDuplicateJobPhaseRunner(jobPhaseNodeVo);
        });
    }

    @Override
    public boolean checkIsAllActivePhaseIsCompleted(Long jobId, Integer sort) {
        boolean isDone = false;
        Integer phaseNotCompletedCount = autoexecJobMapper.getJobPhaseNotCompletedCountByJobIdAndSort(jobId, sort);
        Integer phaseRunnerNotCompletedCount = autoexecJobMapper.getJobPhaseRunnerNotCompletedCountByJobIdAndIsFireNext(jobId, 0, sort);
        if (phaseNotCompletedCount == 0 && phaseRunnerNotCompletedCount == 0) {
            isDone = true;
        }
        return isDone;
    }

    @Override
    public void setIsRefresh(JSONObject paramObj, AutoexecJobVo jobVo, String status) {
        paramObj.put("isRefresh", 1);
        if (Objects.equals(status, JobStatus.COMPLETED.getValue()) && Arrays.asList(JobStatus.COMPLETED.getValue(), JobStatus.FAILED.getValue(), JobStatus.ABORTED.getValue()).contains(jobVo.getStatus())) {
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
}
