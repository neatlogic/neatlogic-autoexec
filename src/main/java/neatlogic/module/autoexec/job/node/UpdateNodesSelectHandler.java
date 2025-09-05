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
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
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
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, null);
            if (MapUtils.isNotEmpty(executeConfigVo.getPreCondition())) {
                searchVo.setPreCondition(JSON.toJavaObject(executeConfigVo.getPreCondition(), ResourceSearchVo.class));
                //存在前置条件是高级模式
                if (searchVo.getPreCondition() != null && searchVo.getPreCondition().isCustomCondition()) {
                    StringBuilder preSqlSb = new StringBuilder();
                    searchVo.getPreCondition().buildConditionWhereSql(preSqlSb, searchVo.getPreCondition());
                    searchVo.getPreCondition().setConditionWhereSql(preSqlSb.toString());
                }
            }
            searchVo.setIdList(nodeVoList.stream().map(AutoexecNodeVo::getId).collect(toList()));
            int count;
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
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
