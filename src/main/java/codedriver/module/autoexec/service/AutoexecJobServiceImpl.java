/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dto.AutoexecRunnerGroupNetworkVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerGroupVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        String executeUser = StringUtils.EMPTY;
        if (nodeConfigVo != null) {
            executeUser = nodeConfigVo.getExecuteUser();
            if (nodeConfigVo.getExecuteNodeConfig() != null) {
                jobNodeVoList = getJobNodeList(nodeConfigVo, jobVo.getOperationId());
            }
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
            }
            AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
            //jobPhaseNode
            //如果是target 则获取执行目标，否则随机分配runner
            if (Objects.equals(autoexecCombopPhaseVo.getExecMode(), ExecMode.TARGET.getValue())) {
                AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = null;
                if (executeConfigVo != null) {
                    jobPhaseNodeVoList = getJobNodeList(executeConfigVo, autoexecCombopPhaseVo.getCombopId());
                }
                if (CollectionUtils.isEmpty(jobPhaseNodeVoList)) {
                    jobPhaseNodeVoList = jobNodeVoList;
                } else if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                    executeUser = executeConfigVo.getExecuteUser();
                }
                for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : jobPhaseNodeVoList) {
                    jobPhaseNodeVo.setJobId(jobVo.getId());
                    jobPhaseNodeVo.setJobPhaseId(jobPhaseVo.getId());
                    Long runnerId = getRunnerByIp(jobPhaseNodeVo.getHost());
                    jobPhaseNodeVo.setStatus(JobNodeStatus.PENDING.getValue());
                    jobPhaseNodeVo.setUserName(executeUser);
                    autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
                    autoexecJobMapper.insertJobPhaseNodeRunner(jobPhaseNodeVo.getId(), runnerId);
                }
            } else {
                List<AutoexecRunnerVo> runnerList = autoexecRunnerMapper.getAllRunner();
                //TODO 负载均衡
                int runnerIndex = (int) (Math.random() * runnerList.size());
                AutoexecRunnerVo autoexecRunnerVo = runnerList.get(runnerIndex);
                AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), jobPhaseVo.getId(), autoexecRunnerVo.getHost(), autoexecRunnerVo.getPort(), JobNodeStatus.PENDING.getValue(), executeUser);
                autoexecJobMapper.insertJobPhaseNode(nodeVo);
                autoexecJobMapper.insertJobPhaseNodeRunner(nodeVo.getId(), autoexecRunnerVo.getId());
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
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndSort(jobId, sort);
        List<AutoexecJobPhaseVo> targetPhaseList = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getExecMode(), ExecMode.TARGET.getValue())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(targetPhaseList)) {
            AutoexecCombopExecuteConfigVo nodeConfigVo = configVo.getExecuteConfig();
            String executeUser = nodeConfigVo.getExecuteUser();
            List<AutoexecJobPhaseNodeVo> jobNodeVoList = null;
            jobNodeVoList = getJobNodeList(nodeConfigVo, jobVo.getOperationId());
            autoexecJobMapper.deleteJobPhaseNodeByJobPhaseIdList(targetPhaseList.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()));
            List<AutoexecCombopPhaseVo> combopPhaseList = configVo.getCombopPhaseList();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (sort != autoexecCombopPhaseVo.getSort() || targetPhaseList.stream().noneMatch(o -> Objects.equals(o.getName(), autoexecCombopPhaseVo.getName()))) {
                    continue;
                }
                AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
                //jobPhaseNode
                AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
                List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = null;
                if (executeConfigVo != null) {
                    jobPhaseNodeVoList = getJobNodeList(executeConfigVo, autoexecCombopPhaseVo.getCombopId());
                }
                if (CollectionUtils.isEmpty(jobPhaseNodeVoList)) {
                    jobPhaseNodeVoList = jobNodeVoList;
                } else if (StringUtils.isNotBlank(executeConfigVo.getExecuteUser())) {
                    executeUser = executeConfigVo.getExecuteUser();
                }
                Long jobPhaseId = null;
                for (AutoexecJobPhaseVo jobPhase : jobPhaseVoList) {
                    if (Objects.equals(jobPhase.getName(), autoexecCombopPhaseVo.getName())) {
                        jobPhaseId = jobPhase.getId();
                    }
                }
                for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : jobPhaseNodeVoList) {
                    jobPhaseNodeVo.setJobId(jobId);
                    jobPhaseNodeVo.setJobPhaseId(jobPhaseId);
                    jobPhaseNodeVo.setRunnerId(getRunnerByIp(jobPhaseNodeVo.getHost()));
                    jobPhaseNodeVo.setStatus(JobNodeStatus.PENDING.getValue());
                    jobPhaseNodeVo.setUserName(executeUser);
                    autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
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
        List<AutoexecRunnerGroupNetworkVo> networkVoList = autoexecRunnerMapper.getAllNetworkMask();
        for (AutoexecRunnerGroupNetworkVo networkVo : networkVoList) {
            if (IpUtil.isBelongSegment(ip, networkVo.getNetworkIp(), networkVo.getMask())) {
                AutoexecRunnerGroupVo groupVo = autoexecRunnerMapper.getRunnerGroupById(networkVo.getGroupId());
                int runnerIndex = (int) (Math.random() * groupVo.getRunnerList().size());
                AutoexecRunnerVo runnerVo = groupVo.getRunnerList().get(runnerIndex);
                return runnerVo.getId();
            }
        }
        return null;
    }

    /**
     * 转化为node
     *
     * @param nodeConfigVo 执行目标node配置
     * @param combopId     组合工具id
     * @return nodeList
     */
    private List<AutoexecJobPhaseNodeVo> getJobNodeList(AutoexecCombopExecuteConfigVo nodeConfigVo, Long combopId) {
        List<AutoexecJobPhaseNodeVo> jobNodeVoList = new ArrayList<AutoexecJobPhaseNodeVo>();
        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = nodeConfigVo.getExecuteNodeConfig();
        String userName = nodeConfigVo.getExecuteUser();
        //tagList
        List<Long> tagList = executeNodeConfigVo.getTagList();
        if (CollectionUtils.isNotEmpty(tagList)) {
            for (Long tag : tagList) {
                //TODO 待资源中心完成后，继续实现标签逻辑
            }
        }
        //inputNodeList、selectNodeList
        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList()) || CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            List<AutoexecNodeVo> nodeVoList = executeNodeConfigVo.getInputNodeList();
            if (CollectionUtils.isEmpty(nodeVoList)) {
                nodeVoList = new ArrayList<>();
            }
            nodeVoList.addAll(executeNodeConfigVo.getSelectNodeList());
            for (AutoexecNodeVo nodeVo : nodeVoList) {
                AutoexecJobPhaseNodeVo jobPhaseNodeVoTmp = new AutoexecJobPhaseNodeVo(nodeVo);
                if (!jobNodeVoList.contains(jobPhaseNodeVoTmp)) {
                    jobNodeVoList.add(jobPhaseNodeVoTmp);
                }
            }
        }
        //paramList
        List<String> paramList = executeNodeConfigVo.getParamList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
            for (AutoexecCombopParamVo paramVo : autoexecCombopParamVoList) {
                AutoexecJobPhaseNodeVo jobPhaseNodeVoTmp = new AutoexecJobPhaseNodeVo(paramVo);
                if (!jobNodeVoList.contains(jobPhaseNodeVoTmp)) {
                    jobNodeVoList.add(jobPhaseNodeVoTmp);
                }
            }
        }
        //TODO 判断该IP:PORT 账号是否存在
        List<String> ipPortErrorList = new ArrayList<>();
        int len = jobNodeVoList.size();
        for (int i = 0; i < len; i++) {
            jobNodeVoList.get(i).setUserName(userName);
            //TODO 待资源中心完成后，继续实现IP:PORT/USER校验逻辑
        }
        return jobNodeVoList;
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
}
