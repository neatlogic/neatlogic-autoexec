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
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Service
public class UpdateNodesSelectHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public boolean update(AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        boolean isHasNode = false;
        if (executeNodeConfigVo == null) {
            return false;
        }

        if (CollectionUtils.isNotEmpty(executeNodeConfigVo.getSelectNodeList())) {
            isHasNode = updateNodeResourceBySelect(jobVo, executeNodeConfigVo, userName, protocolId);
        }
        return isHasNode;
    }

    /**
     * selectNodeList
     * 根据输入和选择节点 更新作业节点
     *
     * @param executeNodeConfigVo 执行节点配置
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceBySelect(AutoexecJobVo jobVo, AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = executeNodeConfigVo.getSelectNodeList();
        boolean isHasNode = false;
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            JSONObject preFilter = null;
            if (Objects.equals(jobVo.getNodeFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())) {
                //如果作业层面的节点则补充前置filter
                AutoexecCombopConfigVo config = jobVo.getConfig();
                if (config != null && config.getExecuteConfig() != null && config.getExecuteConfig().getCombopNodeConfig() != null && MapUtils.isNotEmpty(config.getExecuteConfig().getCombopNodeConfig().getFilter())) {
                    if (Objects.equals(config.getExecuteConfig().getWhenToSpecify(), CombopNodeSpecify.RUNTIME.getValue())) {
                        preFilter = config.getExecuteConfig().getCombopNodeConfig().getFilter();
                    }
                }
            }
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, preFilter);
            searchVo.setIdList(nodeVoList.stream().map(AutoexecNodeVo::getId).collect(toList()));
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
        return isHasNode;
    }
}
