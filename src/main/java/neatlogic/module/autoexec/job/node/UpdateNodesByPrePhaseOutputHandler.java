/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.job.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNodePreParamValueNotInvalidException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseOperationNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobUpdateNodeByPreOutPutListException;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UpdateNodesByPrePhaseOutputHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public boolean update(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        if (CollectionUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getPreOutputList())) {
            return false;
        }
        return updateNodeResourceByPrePhaseOutput(jobVo, executeConfigVo, userName, protocolId);
    }

    /**
     * param
     * 根据上游阶段出参 更新作业节点
     *
     * @param executeConfigVo 执行节点配置
     * @param jobVo           作业
     * @param userName        执行用户
     * @param protocolId      协议id
     */
    private boolean updateNodeResourceByPrePhaseOutput(AutoexecJobVo jobVo, AutoexecCombopExecuteConfigVo executeConfigVo, String userName, Long protocolId) {
        List<String> preOutputList = executeConfigVo.getExecuteNodeConfig().getPreOutputList();
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
        if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(jobVo.getPreOutputPhase().getExecMode())) {
            List<AutoexecJobPhaseNodeVo> nodeList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobVo.getId(), jobVo.getPreOutputPhase().getId());
            doc.put("resourceId", nodeList.get(0).getResourceId());
        } else {
            doc.put("resourceId", 0L);
        }
        doc.put("jobId", jobVo.getId().toString());
        fieldDocument.put("data", true);
        mongoTemplate.getDb().getCollection("_node_output").find(doc).projection(fieldDocument).forEach(o -> {
            JSONObject operation = JSON.parseObject(o.toJson());
            if (operation.containsKey("data")) {
                JSONObject dataJson = operation.getJSONObject("data");
                JSONObject outputJson = dataJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
                if (MapUtils.isNotEmpty(outputJson)) {
                    Object nodes = outputJson.get(paramKey);
                    if (nodes != null) {
                        if (nodes instanceof JSONArray) {
                            nodeArrayAtomic.set((JSONArray) nodes);
                        } else if (nodes instanceof String) {
                            try {
                                nodeArrayAtomic.set(JSON.parseArray(nodes.toString()));
                            } catch (Exception ex) {
                                throw new AutoexecJobNodePreParamValueNotInvalidException(jobVo.getId(), jobVo.getExecutePhase().getName());
                            }
                        } else {
                            throw new AutoexecJobNodePreParamValueNotInvalidException(jobVo.getId(), jobVo.getExecutePhase().getName());
                        }
                    }
                }
            }
        });
        JSONArray nodeArray = nodeArrayAtomic.get();
        if (CollectionUtils.isEmpty(nodeArray)) {
            throw new AutoexecJobNodePreParamValueNotInvalidException(jobVo.getId(), jobVo.getExecutePhase().getName());
        }

        //更新执行节点
        List<AutoexecNodeVo> nodeVoList = nodeArray.toJavaList(AutoexecNodeVo.class);
        List<ResourceVo> ipPortNameList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            nodeVoList.forEach(o -> ipPortNameList.add(new ResourceVo(o.getIp(), o.getPort(), o.getName())));
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, null, executeConfigVo.getPreCondition());
            return autoexecJobService.updateNodeByIpPortNameList(ipPortNameList, searchVo, jobVo, userName, protocolId);
        }
        return false;
    }
}
