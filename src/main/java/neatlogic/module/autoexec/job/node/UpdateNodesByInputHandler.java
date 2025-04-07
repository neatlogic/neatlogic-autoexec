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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.AutoexecJobPhaseNodeFrom;
import neatlogic.framework.autoexec.constvalue.CombopNodeSpecify;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class UpdateNodesByInputHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;
    @Override
    public boolean update(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        boolean isHasNode = false;
        if (executeNodeConfigVo == null) {
            return false;
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getInputNodeList())) {
            isHasNode = updateNodeResourceByInput(executeNodeConfigVo, jobVo, userName, protocolId);
        }
        return  isHasNode;
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
        boolean isHasNode = false;
        List<ResourceVo> ipPortNameList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            nodeVoList.forEach(o -> ipPortNameList.add(new ResourceVo(o.getIp(), o.getPort(), o.getName())));
            JSONObject preFilter = null;
            if (Objects.equals(jobVo.getNodeFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())) {
                //如果作业层面的节点则补充前置filter
                AutoexecCombopConfigVo config = jobVo.getConfig();
                if (config != null && config.getExecuteConfig() != null
                        && config.getExecuteConfig().getCombopNodeConfig() != null
                        && MapUtils.isNotEmpty(config.getExecuteConfig().getCombopNodeConfig().getFilter())
                        && (Objects.equals(config.getExecuteConfig().getWhenToSpecify(), CombopNodeSpecify.RUNTIME.getValue()))) {
                        preFilter = config.getExecuteConfig().getCombopNodeConfig().getFilter();
                }
            }
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, preFilter);
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<ResourceSearchVo> resourceSearchList = new ArrayList<>();
            Set<Long> resourceIdSet = new HashSet<>();
            ipPortNameList.forEach(o -> resourceSearchList.add(new ResourceSearchVo(o)));
            if (CollectionUtils.isNotEmpty(resourceSearchList)) {
                for (ResourceSearchVo resourceSearchVo : resourceSearchList) {
                    Long id = resourceCrossoverMapper.getResourceIdByIpAndPortAndName(resourceSearchVo);
                    if (id != null) {
                        resourceIdSet.add(id);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(resourceIdSet)) {
                searchVo.setIdList(new ArrayList<>(resourceIdSet));
                int count = resourceCrossoverMapper.getResourceCount(searchVo);
                if (count > 0) {
                    int pageCount = PageUtil.getPageCount(count, searchVo.getPageSize());
                    for (int i = 1; i <= pageCount; i++) {
                        searchVo.setCurrentPage(i);
                        List<Long> idList = resourceCrossoverMapper.getResourceIdList(searchVo);
                        if (CollectionUtils.isNotEmpty(idList)) {
                            List<ResourceVo> resourceList = resourceCrossoverMapper.getResourceListByIdList(idList);
                            if (CollectionUtils.isNotEmpty(resourceList)) {
                                autoexecJobService.updateJobPhaseNode(jobVo, resourceList, userName, protocolId);
                                isHasNode = true;
                            }
                        }
                    }
                }
            }
        }
        return isHasNode;
    }
}
