/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dto.AutoexecRunnerGroupNetworkVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerGroupVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerMapVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecJobNodeSshCountNotFoundException;
import codedriver.framework.cmdb.enums.resourcecenter.Protocol;
import codedriver.framework.util.IpUtil;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecRunnerMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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
    private AutoexecCombopService autoexecCombopService;
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    AutoexecRunnerMapper autoexecRunnerMapper;

    @Override
    public AutoexecJobVo saveAutoexecCombopJob(AutoexecCombopVo combopVo, String source, Integer threadCount, JSONObject paramJson) {
        AutoexecCombopConfigVo config = combopVo.getConfig();
        combopVo.setRuntimeParamList(autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopVo.getId()));
        AutoexecJobVo jobVo = new AutoexecJobVo(combopVo, CombopOperationType.COMBOP.getValue(), source, threadCount, paramJson);
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
            protocol = nodeConfigVo.getProtocol();
            //getJobNodeList(nodeConfigVo, jobVo.getId(), jobVo.getOperationId(), userName, protocol);
        }
        //保存阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = new ArrayList<>();
        jobVo.setPhaseList(jobPhaseVoList);
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        for (int i = 0; i < combopPhaseList.size(); i++) {
            AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.get(i);
            AutoexecJobPhaseVo jobPhaseVo = new AutoexecJobPhaseVo(autoexecCombopPhaseVo, i, jobVo.getId());
            autoexecJobMapper.insertJobPhase(jobPhaseVo);
            if (jobPhaseVo.getSort() == 0) {//只需要第一个剧本，供后续激活执行
                jobPhaseVoList.add(jobPhaseVo);
                jobVo.setCurrentPhaseSort(0);
            }
            AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
            //jobPhaseNode
            //如果是target 则获取执行目标，否则随机分配runner
            if (Objects.equals(autoexecCombopPhaseVo.getExecMode(), ExecMode.TARGET.getValue())) {
                AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                int isPhaseSetNode = 0;
                if (executeConfigVo != null) {
                    if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                        userName = executeConfigVo.getExecuteUser();
                    }
                    if (StringUtils.isNotBlank(executeConfigVo.getProtocol())) {
                        protocol = executeConfigVo.getProtocol();
                    }
                    if (executeConfigVo.getExecuteNodeConfig() != null) {
                        isPhaseSetNode = getJobNodeList(executeConfigVo, jobVo.getId(), jobPhaseVo.getId(), jobVo.getOperationId(), userName, protocol);
                    }
                }
                //如果阶段没有设置执行目标，则使用节点执行目标
                if (nodeConfigVo != null && isPhaseSetNode == 0) {
                    getJobNodeList(nodeConfigVo, jobVo.getId(), jobPhaseVo.getId(),jobVo.getOperationId(), userName, protocol);
                }
            } else {
                List<AutoexecRunnerMapVo> runnerMapList = autoexecRunnerMapper.getAllRunnerMap();
                //TODO 负载均衡
                int runnerMapIndex = (int) (Math.random() * runnerMapList.size());
                AutoexecRunnerMapVo autoexecRunnerMapVo = runnerMapList.get(runnerMapIndex);
                AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), jobPhaseVo.getId(), autoexecRunnerMapVo.getHost(), JobNodeStatus.PENDING.getValue(), userName);
                autoexecJobMapper.insertJobPhaseNode(nodeVo);
                autoexecRunnerMapper.insertRunnerMap(autoexecRunnerMapVo);
                autoexecJobMapper.insertJobPhaseNodeRunner(nodeVo.getId(), autoexecRunnerMapVo.getRunnerMapId());
                autoexecJobMapper.insertJobPhaseRunner(nodeVo.getJobPhaseId(),autoexecRunnerMapVo.getRunnerMapId());
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
                    AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
                    AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
                    jobPhaseOperationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, scriptVo, scriptVersionVo, autoexecCombopService.getOperationActiveVersionScriptByOperationId(operationId), jobPhaseVoList);
                    autoexecJobMapper.insertJobPhaseOperation(jobPhaseOperationVo);
                    autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(jobPhaseOperationVo.getParamHash(), jobPhaseOperationVo.getParamStr()));
                    jobPhaseOperationVoList.add(jobPhaseOperationVo);
                }
            }
        }
        return jobVo;
    }

    @Override
    public void refreshJobPhaseNodeList(Long jobId, int sort) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        AutoexecCombopConfigVo configVo = JSON.toJavaObject(jobVo.getConfig(), AutoexecCombopConfigVo.class);
        //获取当前所有target阶段
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndSort(jobId, sort);
        List<AutoexecJobPhaseVo> targetPhaseList = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getExecMode(), ExecMode.TARGET.getValue())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(targetPhaseList)) {
            //获取组合工具执行目标 执行用户和协议
            AutoexecCombopExecuteConfigVo nodeConfigVo = configVo.getExecuteConfig();
            String userName = StringUtils.EMPTY;
            String protocol = StringUtils.EMPTY;
            if (nodeConfigVo != null) {
                userName = nodeConfigVo.getExecuteUser();
                protocol = nodeConfigVo.getProtocol();
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
                if(CollectionUtils.isNotEmpty(jobPhaseVos)){
                    AutoexecJobPhaseVo jobPhaseVo= jobPhaseVos.get(0);
                    jobPhaseVo.setCombopId(jobVo.getOperationId());
                    AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                    int isPhaseSetNode = 0;
                    if (executeConfigVo != null) {
                        if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                            userName = executeConfigVo.getExecuteUser();
                        }
                        if (StringUtils.isNotBlank(executeConfigVo.getProtocol())) {
                            protocol = executeConfigVo.getProtocol();
                        }
                        if (executeConfigVo.getExecuteNodeConfig() != null) {
                            isPhaseSetNode = getJobNodeList(executeConfigVo, jobVo.getId(), jobPhaseVo.getId(), jobVo.getOperationId(), userName, protocol);
                        }
                    }
                    if (nodeConfigVo != null && isPhaseSetNode == 0) {
                        getJobNodeList(nodeConfigVo, jobVo.getId(), jobPhaseVo.getId(),jobVo.getOperationId(), userName, protocol);
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
    private Integer getRunnerByIp(String ip) {
        List<AutoexecRunnerGroupNetworkVo> networkVoList = autoexecRunnerMapper.getAllNetworkMask();
        for (AutoexecRunnerGroupNetworkVo networkVo : networkVoList) {
            if (IpUtil.isBelongSegment(ip, networkVo.getNetworkIp(), networkVo.getMask())) {
                AutoexecRunnerGroupVo groupVo = autoexecRunnerMapper.getRunnerGroupById(networkVo.getGroupId());
                int runnerMapIndex = (int) (Math.random() * groupVo.getRunnerMapList().size());
                AutoexecRunnerMapVo runnerMapVo = groupVo.getRunnerMapList().get(runnerMapIndex);
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
    private int getJobNodeList(AutoexecCombopExecuteConfigVo nodeConfigVo, Long jobId, Long phaseId, Long combopId, String userName, String protocol) {
        int isHasNode = 0;

        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = nodeConfigVo.getExecuteNodeConfig();
        //tagList
        List<Long> tagList = executeNodeConfigVo.getTagList();
        if (CollectionUtils.isNotEmpty(tagList)) {
            for (Long tag : tagList) {
                //TODO 待资源中心完成后，继续实现标签逻辑
            }
            isHasNode = 1;
        }

        //inputNodeList、selectNodeList
        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList()) || CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            List<AutoexecNodeVo> nodeVoList = executeNodeConfigVo.getInputNodeList();
            nodeVoList.addAll(executeNodeConfigVo.getSelectNodeList());
            //ssh 需根据IP protocol 和 username 获取对应的port 和 name
            if(Objects.equals(Protocol.SSH.getValue(),protocol)){
                List<String> nodeIpList = new ArrayList<String>();
                List<AutoexecJobNodeVo> autoexecJobVoList = autoexecJobMapper.getJobPhaseNodePortByIpAndUserNameAndProtocol(nodeVoList.stream().map(AutoexecNodeVo::getIp).collect(Collectors.toList()), userName, protocol, TenantContext.get().getDataDbName());
                Map<String,Integer> nodePortMap = new HashMap<>();
                Map<String,String> nodeNameMap = new HashMap<>();
                autoexecJobVoList.forEach(o->{
                    nodePortMap.put(o.getHost(),o.getPort());
                    nodeNameMap.put(o.getHost(),o.getNodeName());
                });
                for (AutoexecNodeVo nodeVo : nodeVoList) {
                    if (!nodeIpList.contains(nodeVo.getIp())) {
                        nodeIpList.add(nodeVo.getIp());
                        if (phaseId != null) {
                            AutoexecJobPhaseNodeVo jobPhaseNodeVo = new AutoexecJobPhaseNodeVo(nodeVo, jobId, phaseId, JobNodeStatus.PENDING.getValue(), userName, protocol);
                            if(!nodePortMap.containsKey(nodeVo.getIp())){
                                throw new AutoexecJobNodeSshCountNotFoundException(userName+"@"+nodeVo.getIp());
                            }
                            jobPhaseNodeVo.setPort(nodePortMap.get(nodeVo.getIp()));
                            jobPhaseNodeVo.setNodeName(nodeNameMap.get(nodeVo.getIp()));
                            jobPhaseNodeVo.setRunnerMapId(getRunnerByIp(jobPhaseNodeVo.getHost()));
                            autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
                            autoexecJobMapper.insertJobPhaseNodeRunner(jobPhaseNodeVo.getId(), jobPhaseNodeVo.getRunnerMapId());
                            autoexecJobMapper.insertJobPhaseRunner(jobPhaseNodeVo.getJobPhaseId(),jobPhaseNodeVo.getRunnerMapId());
                            isHasNode = 1;
                        } else {
                            //TODO 没有意义？ 作业的执行节点，因为有tag，节点可能会变
//                            AutoexecJobNodeVo jobNodeVo = new AutoexecJobNodeVo(nodeVo.getIp(), jobId, userName, protocol);
//                            autoexecJobMapper.insertJobNode(jobNodeVo);
                        }
                    }

                }
            }else{//TODO 其它协议
                for (AutoexecNodeVo nodeVo : nodeVoList) {
                    if (phaseId != null) {
                        AutoexecJobPhaseNodeVo jobPhaseNodeVo = new AutoexecJobPhaseNodeVo(nodeVo, jobId, phaseId, JobNodeStatus.PENDING.getValue(), userName, protocol);
                        jobPhaseNodeVo.setRunnerMapId(getRunnerByIp(jobPhaseNodeVo.getHost()));
                        autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
                        autoexecJobMapper.insertJobPhaseNodeRunner(jobPhaseNodeVo.getId(), jobPhaseNodeVo.getRunnerMapId());
                        autoexecJobMapper.insertJobPhaseRunner(jobPhaseNodeVo.getJobPhaseId(),jobPhaseNodeVo.getRunnerMapId());
                        isHasNode = 1;
                    } else {
                        //TODO 没有意义？ 作业的执行节点，因为有tag，节点可能会变
//                            AutoexecJobNodeVo jobNodeVo = new AutoexecJobNodeVo(nodeVo.getIp(), jobId, userName, protocol);
//                            autoexecJobMapper.insertJobNode(jobNodeVo);
                    }
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
            }
            isHasNode = 1;
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
    public void setIsRefresh(JSONObject paramObj, Long jobId) {
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobId);
        paramObj.put("isRefresh", 0);
        for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
            if (Objects.equals(phaseVo.getStatus(), JobPhaseStatus.RUNNING.getValue()) || Objects.equals(phaseVo.getStatus(), JobPhaseStatus.PENDING.getValue()) || Objects.equals(phaseVo.getStatus(), JobPhaseStatus.WAITING.getValue())) {
                paramObj.put("isRefresh", 1);
                break;
            }
        }
    }
}
