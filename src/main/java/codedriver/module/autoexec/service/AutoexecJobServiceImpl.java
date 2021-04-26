/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.AutoexecProxyGroupNetworkVo;
import codedriver.framework.autoexec.dto.AutoexecProxyGroupVo;
import codedriver.framework.autoexec.dto.AutoexecProxyVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.util.IpUtil;
import codedriver.module.autoexec.dao.mapper.*;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    AutoexecProxyMapper autoexecProxyMapper;

    @Override
    public void saveAutoexecCombopJob(AutoexecCombopVo combopVo, String source, Integer threadCount, JSONObject paramJson) {
        AutoexecCombopConfigVo config = combopVo.getConfig();
        combopVo.setRuntimeParamList(autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopVo.getId()));
        AutoexecJobVo jobVo = new AutoexecJobVo(combopVo, CombopOperationType.COMBOP.getValue(), source, threadCount, paramJson);
        //保存作业基本信息
        autoexecJobMapper.insertJob(jobVo);
        autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(jobVo.getParamHash(), jobVo.getParamStr()));
        //保存作业执行目标
        AutoexecCombopExecuteConfigVo nodeConfigVo = config.getExecuteConfig();
        List<AutoexecJobPhaseNodeVo> jobNodeVoList = null;
        if (nodeConfigVo != null) {
            jobNodeVoList = getJobNodeList(nodeConfigVo, jobVo.getOperationId());
        }
        //保存阶段
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        for (int i = 0; i < combopPhaseList.size(); i++) {
            AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.get(i);
            AutoexecJobPhaseVo jobPhaseVo = new AutoexecJobPhaseVo(autoexecCombopPhaseVo, i, jobVo.getId());
            autoexecJobMapper.insertJobPhase(jobPhaseVo);
            AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
            //jobPhaseNode
            AutoexecCombopExecuteConfigVo executeConfigVo = phaseConfigVo.getExecuteConfig();
            List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = null;
            if (executeConfigVo != null) {
                jobPhaseNodeVoList = getJobNodeList(executeConfigVo, autoexecCombopPhaseVo.getCombopId());
            }
            if (CollectionUtils.isEmpty(jobPhaseNodeVoList)) {
                jobPhaseNodeVoList = jobNodeVoList;
            }
            for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : jobPhaseNodeVoList) {
                jobPhaseNodeVo.setJobId(jobVo.getId());
                jobPhaseNodeVo.setJobPhaseId(jobPhaseVo.getId());
                jobPhaseNodeVo.setProxyId(getProxyByIp(jobPhaseNodeVo.getHost()));
                autoexecJobMapper.insertJobPhaseNode(jobPhaseNodeVo);
            }
            //jobPhaseOperation
            List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = phaseConfigVo.getPhaseOperationList();
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : combopPhaseOperationList) {
                String operationType = autoexecCombopPhaseOperationVo.getOperationType();
                Long operationId = autoexecCombopPhaseOperationVo.getOperationId();
                AutoexecJobPhaseOperationVo operationVo = null;
                if (CombopOperationType.SCRIPT.getValue().equalsIgnoreCase(operationType)) {
                    AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
                    AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
                    List<AutoexecScriptLineVo> scriptLineVoList = autoexecScriptMapper.getLineListByVersionId(scriptVersionVo.getId());
                    operationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, scriptVo, scriptVersionVo, scriptLineVoList);
                    autoexecJobMapper.insertJobPhaseOperation(operationVo);
                    autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(operationVo.getParamHash(), operationVo.getParamStr()));
                }
            }
        }
    }

    /**
     * 根据目标ip自动匹配proxy
     *
     * @param ip 目标ip
     * @return proxyId
     */
    private Long getProxyByIp(String ip) {
        List<AutoexecProxyGroupNetworkVo> networkVoList = autoexecProxyMapper.getAllNetworkMask();
        for (AutoexecProxyGroupNetworkVo networkVo : networkVoList) {
            if (IpUtil.isBelongSegment(ip, networkVo.getNetworkIp(), networkVo.getMask())) {
                AutoexecProxyGroupVo groupVo = autoexecProxyMapper.getProxyGroupById(networkVo.getProxyGroupId());
                int proxyIndex = (int) (Math.random() * groupVo.getProxyList().size());
                AutoexecProxyVo proxyVo = groupVo.getProxyList().get(proxyIndex);
                return proxyVo.getId();
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
        List<String> tagList = executeNodeConfigVo.getTagList();
        if (CollectionUtils.isNotEmpty(tagList)) {
            for (String tag : tagList) {
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
}
