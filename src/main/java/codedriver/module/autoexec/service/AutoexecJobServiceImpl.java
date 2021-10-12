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
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerGroupRunnerNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotMatchException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterResourceHasRepeatAccountException;
import codedriver.framework.common.util.IpUtil;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.dto.runner.GroupNetworkVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @since 2021/4/12 18:44
 **/
@Service
public class AutoexecJobServiceImpl implements AutoexecJobService {
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
        invokeVo.setJobId(jobVo.getId());
        if(!Objects.equals(JobSource.HUMAN.getValue(),invokeVo.getSource())) {
            autoexecJobMapper.insertIgnoreIntoJobInvoke(invokeVo);
        }
        //保存作业基本信息
        autoexecJobMapper.insertJob(jobVo);
        autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(jobVo.getParamHash(), jobVo.getParamStr()));
        //保存作业执行目标
        AutoexecCombopExecuteConfigVo nodeConfigVo = config.getExecuteConfig();
        List<AutoexecJobPhaseNodeVo> jobNodeVoList = null;
        String userName = StringUtils.EMPTY;
        String protocol = StringUtils.EMPTY;
        if (nodeConfigVo != null) {
            userName = nodeConfigVo.getExecuteUser();
            protocol = nodeConfigVo.getProtocolId().toString();
            //getJobNodeList(nodeConfigVo, jobVo.getId(), jobVo.getOperationId(), userName, protocol);
        }
        //保存阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = new ArrayList<>();
        jobVo.setPhaseList(jobPhaseVoList);
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            AutoexecJobPhaseVo jobPhaseVo = new AutoexecJobPhaseVo(autoexecCombopPhaseVo, jobVo.getId());
            autoexecJobMapper.insertJobPhase(jobPhaseVo);
            if (jobPhaseVo.getSort() == 0) {//只需要第一个剧本，供后续激活执行
                jobPhaseVoList.add(jobPhaseVo);
                jobVo.setCurrentPhaseSort(0);
            }
            AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
            //jobPhaseNode
            //如果是target 则获取执行目标，否则随机分配runner
            if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue(), ExecMode.SQL.getValue()).contains(autoexecCombopPhaseVo.getExecMode())) {
                AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                boolean isPhaseSetNode = false;
                if (executeConfigVo != null) {
                    if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                        userName = executeConfigVo.getExecuteUser();
                    }
                    if (executeConfigVo.getProtocolId() != null) {
                        protocol = executeConfigVo.getProtocolId().toString();
                    }
                    if (executeConfigVo.getExecuteNodeConfig() != null) {
                        isPhaseSetNode = getJobNodeList(executeConfigVo, jobVo.getId(), jobPhaseVo.getId(), jobVo.getOperationId(), userName, protocol).get();
                    }
                }
                //如果阶段没有设置执行目标，则使用节点执行目标
                if (nodeConfigVo != null && (executeConfigVo == null || executeConfigVo.getExecuteNodeConfig() == null || (CollectionUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getInputNodeList()) && CollectionUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getSelectNodeList())))) {
                    isPhaseSetNode = getJobNodeList(nodeConfigVo, jobVo.getId(), jobPhaseVo.getId(), jobVo.getOperationId(), userName, protocol).get();
                }
                //如果该阶段没有可以执行的node
                if (!isPhaseSetNode) {
                    throw new AutoexecJobPhaseNodeNotFoundException(jobPhaseVo.getName());
                }
            } else {
                List<RunnerMapVo> runnerMapList = runnerMapper.getAllRunnerMap();
                //TODO 负载均衡
                int runnerMapIndex = (int) (Math.random() * runnerMapList.size());
                RunnerMapVo runnerMapVo = runnerMapList.get(runnerMapIndex);
                AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), jobPhaseVo.getId(), "runner", JobNodeStatus.PENDING.getValue(), userName, protocol);
                autoexecJobMapper.insertJobPhaseNode(nodeVo);
                autoexecJobMapper.insertJobPhaseNodeRunner(nodeVo.getId(), runnerMapVo.getRunnerMapId());
                autoexecJobMapper.insertJobPhaseRunner(nodeVo.getJobId(), nodeVo.getJobPhaseId(), runnerMapVo.getRunnerMapId());
            }
            //jobPhaseOperation
            List<AutoexecJobPhaseOperationVo> jobPhaseOperationVoList = new ArrayList<>();
            jobPhaseVo.setOperationList(jobPhaseOperationVoList);
            List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = phaseConfigVo.getPhaseOperationList();
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
                autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(jobPhaseOperationVo.getParamHash(), jobPhaseOperationVo.getParamStr()));
                jobPhaseOperationVoList.add(jobPhaseOperationVo);
            }
        }
        return jobVo;
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, int sort,AutoexecCombopExecuteConfigVo executeConfigVo) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        AutoexecCombopConfigVo configVo = JSON.toJavaObject(jobVo.getConfig(), AutoexecCombopConfigVo.class);
        //获取当前所有target阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndSort(jobId, sort);
        List<AutoexecJobPhaseVo> targetPhaseList = jobPhaseVoList.stream().filter(o -> Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.SQL.getValue()).contains(o.getExecMode())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(targetPhaseList)) {
            //获取组合工具执行目标 执行用户和协议
            //非空场景，用于重跑替换执行配置（执行目标，用户，协议）
            if(executeConfigVo == null) {
                executeConfigVo = configVo.getExecuteConfig();
            }
            String userName = StringUtils.EMPTY;
            String protocol = StringUtils.EMPTY;
            if (executeConfigVo != null) {
                userName = executeConfigVo.getExecuteUser();
                protocol = executeConfigVo.getProtocolId().toString();
            }
            //删除所有target阶段的节点
            autoexecJobMapper.deleteJobPhaseNodeByJobPhaseIdList(targetPhaseList.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()));
            List<AutoexecCombopPhaseVo> combopPhaseList = configVo.getCombopPhaseList();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                //只刷新当前target阶段
                if (sort != autoexecCombopPhaseVo.getSort() || targetPhaseList.stream().noneMatch(o -> Objects.equals(o.getName(), autoexecCombopPhaseVo.getName()))) {
                    continue;
                }
                AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
                List<AutoexecJobPhaseVo> jobPhaseVos = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getName(), autoexecCombopPhaseVo.getName())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(jobPhaseVos)) {
                    AutoexecJobPhaseVo jobPhaseVo = jobPhaseVos.get(0);
                    jobPhaseVo.setCombopId(jobVo.getOperationId());
                    AutoexecCombopExecuteConfigVo phaseExecuteConfigVo = phaseConfigVo.getExecuteConfig();
                    boolean isPhaseSetNode = false;
                    if (phaseExecuteConfigVo != null) {
                        if (StringUtils.isNotBlank(phaseExecuteConfigVo.getExecuteUser())) {
                            userName = phaseExecuteConfigVo.getExecuteUser();
                        }
                        if (phaseExecuteConfigVo.getProtocolId()!=null) {
                            protocol = phaseExecuteConfigVo.getProtocolId().toString();
                        }
                        if (phaseExecuteConfigVo.getExecuteNodeConfig() != null) {
                            isPhaseSetNode = getJobNodeList(phaseExecuteConfigVo, jobVo.getId(), jobPhaseVo.getId(), jobVo.getOperationId(), userName, protocol).get();
                        }
                    }

                    if (executeConfigVo != null && (phaseExecuteConfigVo == null || phaseExecuteConfigVo.getExecuteNodeConfig() == null || (CollectionUtils.isEmpty(phaseExecuteConfigVo.getExecuteNodeConfig().getInputNodeList()) && CollectionUtils.isEmpty(phaseExecuteConfigVo.getExecuteNodeConfig().getSelectNodeList())))) {
                        isPhaseSetNode = getJobNodeList(executeConfigVo, jobVo.getId(), jobPhaseVo.getId(), jobVo.getOperationId(), userName, protocol).get();
                    }
                    //如果该阶段没有可以执行的node
                    if (!isPhaseSetNode) {
                        throw new AutoexecJobPhaseNodeNotFoundException(jobPhaseVo.getName());
                    }
                }
            }
        }
    }

    @Override
    public void getAutoexecJobDetail(AutoexecJobVo jobVo, Integer sort) {
        AutoexecJobParamContentVo paramContentVo = autoexecJobMapper.getJobParamContent(jobVo.getParamHash());
        if (paramContentVo != null) {
            jobVo.setParamStr(paramContentVo.getContent());
        }
        List<AutoexecJobPhaseVo> phaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId());
        phaseVoList = phaseVoList.stream().filter(o -> Objects.equals(o.getSort(), sort)).collect(Collectors.toList());
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
                RunnerGroupVo groupVo = runnerMapper.getRunnerGroupById(networkVo.getGroupId());
                int runnerMapIndex = (int) (Math.random() * groupVo.getRunnerMapList().size());
                if(CollectionUtils.isEmpty(groupVo.getRunnerMapList())){
                    throw new AutoexecJobRunnerGroupRunnerNotFoundException(networkVo.getGroupId().toString());
                }
                RunnerMapVo runnerMapVo = groupVo.getRunnerMapList().get(runnerMapIndex);
                if(runnerMapVo.getRunnerMapId() == null){
                    runnerMapVo.setRunnerMapId(runnerMapVo.getId());
                    runnerMapper.insertRunnerMap(runnerMapVo);
                }
                return runnerMapVo.getRunnerMapId();
            }
        }
        return null;
    }

    private void getJobNodeList(AutoexecCombopExecuteConfigVo nodeConfigVo, Long jobId, Long combopId, String userName, String protocol) {
        getJobNodeList(nodeConfigVo, jobId, null, combopId, userName, protocol);
    }

    /**
     * @param nodeConfigVo node配置config
     * @param jobId        作业id
     * @param phaseId      作业剧本id
     * @param combopId     组合工具id
     * @param userName     连接node 用户
     * @param protocol     连接node 协议
     * @return 是否有node
     */
    private AtomicBoolean getJobNodeList(AutoexecCombopExecuteConfigVo nodeConfigVo, Long jobId, Long phaseId, Long combopId, String userName, String protocol) {
        AtomicBoolean isHasNode = new AtomicBoolean(false);
        Map<Long, ResourceVo> resourceMap = new HashMap<>();
        List<Long> resourceIdList = new ArrayList<>();
        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = nodeConfigVo.getExecuteNodeConfig();
        //tagList
        List<Long> tagIdList = executeNodeConfigVo.getTagList();
        if (CollectionUtils.isNotEmpty(tagIdList)) {
            List<Long> resourceIdListTmp = resourceCenterMapper.getResourceIdListByTagIdList(new ResourceSearchVo(tagIdList));
            if (CollectionUtils.isNotEmpty(resourceIdList)) {
                resourceIdList.addAll(resourceIdListTmp);
            }
        }
        //inputNodeList、selectNodeList
        List<AutoexecNodeVo> nodeVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList())) {
            nodeVoList.addAll(executeNodeConfigVo.getInputNodeList());
        }
        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            nodeVoList.addAll(executeNodeConfigVo.getSelectNodeList());
        }
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            List<ResourceVo> ipPortList = new ArrayList<>();
            nodeVoList.forEach(o -> {
                ipPortList.add(new ResourceVo(o.getIp(), o.getPort()));
            });
            List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceListByResourceVoList(ipPortList, TenantContext.get().getDataDbName());
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                resourceVoList.forEach(o -> {
                    resourceMap.put(o.getId(), o);
                });
                List<Long> resourceIdListTmp = resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(resourceIdListTmp)) {
                    resourceIdList.addAll(resourceIdListTmp);
                }
            }
        }

        //paramList
        List<String> paramList = executeNodeConfigVo.getParamList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
            for (AutoexecCombopParamVo paramVo : autoexecCombopParamVoList) {
                AutoexecJobPhaseNodeVo jobPhaseNodeVoTmp = new AutoexecJobPhaseNodeVo(paramVo);
                /*if (!nodeIpList.contains(jobPhaseNodeVoTmp.getHost())) {
                    nodeIpList.add(jobPhaseNodeVoTmp.getHost());
                }*/
                //TODO
            }
        }

        //找到资源账号的才存到db
        if (CollectionUtils.isNotEmpty(resourceIdList)) {
            List<AccountVo> accountVoList = resourceCenterMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(resourceIdList, protocol, userName);
            List<String> checkRepeatList = new ArrayList<>();
            accountVoList.forEach(o -> {
                if (resourceMap.containsKey(o.getResourceId())) {
                    String key = o.getResourceId().toString() + o.getProtocol() + o.getAccount();
                    if (checkRepeatList.contains(key)) {
                        throw new ResourceCenterResourceHasRepeatAccountException(o.getResourceId().toString(), o.getProtocol(), o.getAccount());
                    }
                    isHasNode.set(true);
                    ResourceVo resourceVo = resourceMap.get(o.getResourceId());
                    AutoexecJobPhaseNodeVo jobPhaseNodeVo = new AutoexecJobPhaseNodeVo(resourceVo, jobId, phaseId, JobNodeStatus.PENDING.getValue(), userName, protocol, o.getPort());
                    jobPhaseNodeVo.setPort(resourceVo.getPort());
                    jobPhaseNodeVo.setRunnerMapId(getRunnerByIp(jobPhaseNodeVo.getHost()));
                    if(jobPhaseNodeVo.getRunnerMapId() == null){
                        throw new AutoexecJobRunnerNotMatchException(jobPhaseNodeVo.getHost());
                    }
                    autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
                    autoexecJobMapper.insertJobPhaseNodeRunner(jobPhaseNodeVo.getId(), jobPhaseNodeVo.getRunnerMapId());
                    autoexecJobMapper.insertJobPhaseRunner(jobPhaseNodeVo.getJobId(), jobPhaseNodeVo.getJobPhaseId(), jobPhaseNodeVo.getRunnerMapId());
                    checkRepeatList.add(key);
                }
            });
        }

        return isHasNode;
    }

    public void authParam(AutoexecCombopVo combopVo, JSONObject paramJson) {
        List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopVo.getId());
    }

    @Override
    public boolean checkIsAllActivePhaseIsCompleted(Long jobId, Integer sort) {
        boolean isDone = false;
        Integer count = autoexecJobMapper.checkIsAllActivePhaseIsDone(jobId, sort);
        if (count == 0) {
            isDone = true;
        }
        return true;
    }

    @Override
    public void setIsRefresh(JSONObject paramObj, AutoexecJobVo jobVo) {
        paramObj.put("isRefresh", 1);
        if (Objects.equals(jobVo.getStatus(), JobStatus.COMPLETED.getValue())
                || Objects.equals(jobVo.getStatus(), JobStatus.FAILED.getValue())
                || Objects.equals(jobVo.getStatus(), JobStatus.ABORTED.getValue())) {
            paramObj.put("isRefresh", 0);
        }
        /*List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobId);
        paramObj.put("isRefresh", 0);
        for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
            if (Objects.equals(phaseVo.getStatus(), JobPhaseStatus.RUNNING.getValue()) || Objects.equals(phaseVo.getStatus(), JobPhaseStatus.PENDING.getValue()) || Objects.equals(phaseVo.getStatus(), JobPhaseStatus.WAITING.getValue())) {
                paramObj.put("isRefresh", 1);
                break;
            }
        }*/
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
        autoexecJobMapper.deleteJobPhaseRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseOperationByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseByJobId(jobId);
        autoexecJobMapper.deleteJobByJobId(jobId);
    }
}
