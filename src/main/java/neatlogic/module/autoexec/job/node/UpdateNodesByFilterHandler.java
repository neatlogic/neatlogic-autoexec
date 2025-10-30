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
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UpdateNodesByFilterHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;
    private static final Logger logger = LoggerFactory.getLogger(UpdateNodesByFilterHandler.class);

    @Override
    public boolean update(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        if (MapUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getFilter())) {
            return false;
        }
        logger.debug("##updateNodeResourceByFilter:-------------------------------------------------------------------------------start");
        //long updateNodeResourceByFilter = System.currentTimeMillis();
        boolean isHasNode = updateNodeResourceByFilter(executeConfigVo, jobVo, userName, protocolId);
        //System.out.println((System.currentTimeMillis() - updateNodeResourceByFilter) + " ##updateNodeResourceByFilter:-------------------------------------------------------------------------------");
        logger.debug("##updateNodeResourceByFilter:-------------------------------------------------------------------------------end");
        return isHasNode;
    }

    /**
     * filter
     * 根据过滤器 更新节点
     *
     * @param executeConfigVo 执行节点配置
     * @param jobVo           作业
     * @param userName        执行用户
     * @param protocolId      协议id
     */
    public boolean updateNodeResourceByFilter(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        JSONObject filterJson = executeConfigVo.getExecuteNodeConfig().getFilter();
        boolean isHasNode = false;
        if (MapUtils.isNotEmpty(filterJson)) {
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, filterJson, executeConfigVo.getPreCondition());
            searchVo.setMaxPageSize(50000);
            searchVo.setPageSize(50000);
            IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            List<Long> idList = resourceCenterResourceCrossoverService.getResourceIdList(searchVo);
            int count = idList.size();
            if (count > 0) {
                int index = 0;
                List<Long> idPageList = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    if (index < 1000) {
                        idPageList.add(idList.get(i));
                        index++;
                        continue;
                    }
                    i--;
                    logger.debug("##getResourceListByIdList:-------------------------------------------------------------------------------start");
                    //long bbb = System.currentTimeMillis();
                    List<ResourceVo> resourceList = resourceCenterResourceCrossoverService.getResourceListByIdList(idPageList);
                    //System.out.println((System.currentTimeMillis() - bbb) + " ##bbb:-------------------------------------------------------------------------------");
                    logger.debug("##getResourceListByIdList:-------------------------------------------------------------------------------end");
                    if (CollectionUtils.isNotEmpty(resourceList)) {
                        logger.debug("##updateJobPhaseNode:-------------------------------------------------------------------------------start");
                        //long updateJobPhaseNode = System.currentTimeMillis();
                        autoexecJobService.updateJobPhaseNode(jobVo, resourceList, userName, protocolId);
                        // System.out.println((System.currentTimeMillis() - updateJobPhaseNode) + " ##updateJobPhaseNode:-------------------------------------------------------------------------------");
                        logger.debug("##updateJobPhaseNode:-------------------------------------------------------------------------------end");
                    }
                    index = 0;
                    idPageList.clear();
                }
                //补充最后一次循环数据
                if (CollectionUtils.isNotEmpty(idPageList)) {
                    logger.debug("##getResourceListByIdList last:-------------------------------------------------------------------------------start");
                    //long bbb = System.currentTimeMillis();
                    List<ResourceVo> resourceList = resourceCenterResourceCrossoverService.getResourceListByIdList(idPageList);
                    //System.out.println((System.currentTimeMillis() - bbb) + " ##bbb:-------------------------------------------------------------------------------");
                    logger.debug("##getResourceListByIdList last:-------------------------------------------------------------------------------end");
                    if (CollectionUtils.isNotEmpty(resourceList)) {
                        logger.debug("##updateJobPhaseNode last:-------------------------------------------------------------------------------start");
                        //long updateJobPhaseNode = System.currentTimeMillis();
                        autoexecJobService.updateJobPhaseNode(jobVo, resourceList, userName, protocolId);
                        //System.out.println((System.currentTimeMillis() - updateJobPhaseNode) + " ##updateJobPhaseNode:-------------------------------------------------------------------------------");
                        logger.debug("##updateJobPhaseNode last:-------------------------------------------------------------------------------end");
                    }
                    idPageList.clear();
                }
                isHasNode = true;
            }
        }
        return isHasNode;
    }


}
