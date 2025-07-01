/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.job.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNodePreParamValueNotInvalidException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseOperationNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobUpdateNodeByPreOutPutListException;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
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
    public boolean update(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        boolean isHasNode = false;
        if (executeNodeConfigVo == null) {
            return false;
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getPreOutputList())) {
            isHasNode = updateNodeResourceByPrePhaseOutput(jobVo, executeNodeConfigVo, userName, protocolId);
        }
        return isHasNode;
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
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo);
            List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceListByResourceVoList(ipPortNameList, searchVo);
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                autoexecJobService.updateJobPhaseNode(jobVo, resourceVoList, userName, protocolId);
                //重置节点状态
                //List<AutoexecJobPhaseNodeVo> jobNodeVoList = autoexecJobMapper.getJobPhaseNodeListWithRunnerByJobPhaseIdAndExceptStatusList(jobVo.getCurrentPhase().getId(), Collections.singletonList(JobNodeStatus.IGNORED.getValue()));
                //resetJobNodeStatus(jobVo, jobNodeVoList);
                return true;
            }
        }
        return false;
    }
}
