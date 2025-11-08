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

import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class UpdateNodesSelectHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public boolean update(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        if (CollectionUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getSelectNodeList())) {
            return false;
        }
        return updateNodeResourceBySelect(jobVo, executeConfigVo, userName, protocolId);
    }

    /**
     * selectNodeList
     * 根据输入和选择节点 更新作业节点
     *
     * @param executeConfigVo 执行节点配置
     * @param jobVo           作业
     * @param userName        执行用户
     * @param protocolId      协议id
     */
    private boolean updateNodeResourceBySelect(AutoexecJobVo jobVo, AutoexecCombopExecuteConfigVo executeConfigVo, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = executeConfigVo.getExecuteNodeConfig().getSelectNodeList();
        boolean isHasNode = false;
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, null, executeConfigVo.getPreCondition());
            searchVo.setIdList(nodeVoList.stream().map(AutoexecNodeVo::getId).collect(toList()));
            int count;
            IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            count = resourceCenterResourceCrossoverService.getResourceCount(searchVo);
            if (count > 0) {
                int pageCount = PageUtil.getPageCount(count, searchVo.getPageSize());
                for (int i = 1; i <= pageCount; i++) {
                    searchVo.setCurrentPage(i);
                    List<Long> idList = resourceCenterResourceCrossoverService.getResourceIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        List<ResourceVo> resourceList = resourceCenterResourceCrossoverService.getResourceListByIdList(idList);
                        if (CollectionUtils.isNotEmpty(resourceList)) {
                            autoexecJobService.updateJobPhaseNode(jobVo, resourceList, userName, protocolId);
                            isHasNode = true;
                        }
                    }
                }
            }
        }
        return isHasNode;
    }


}
