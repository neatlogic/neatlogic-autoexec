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
import neatlogic.framework.autoexec.exception.job.AutoexecInputOutOfCountException;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UpdateNodesByInputHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public boolean update(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        if (CollectionUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getInputNodeList())) {
            return false;
        }
        return updateNodeResourceByInput(executeConfigVo, jobVo, userName, protocolId);
    }

    /**
     * inputNodeList、selectNodeList
     * 根据输入和选择节点 更新作业节点
     *
     * @param executeConfigVo 执行节点配置
     * @param jobVo           作业
     * @param userName        执行用户
     * @param protocolId      协议id
     */
    private boolean updateNodeResourceByInput(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = executeConfigVo.getExecuteNodeConfig().getInputNodeList();
        boolean isHasNode = false;
        List<ResourceVo> ipPortNameList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            if (nodeVoList.size() > 1000) {
                throw new AutoexecInputOutOfCountException(1000);
            }
            nodeVoList.forEach(o -> ipPortNameList.add(new ResourceVo(o.getIp(), o.getPort(), o.getName())));
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, null, executeConfigVo.getPreCondition());
            isHasNode = autoexecJobService.updateNodeByIpPortNameList(ipPortNameList, searchVo, jobVo, userName, protocolId);
        }
        return isHasNode;
    }
}
