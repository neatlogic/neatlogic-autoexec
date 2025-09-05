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
import neatlogic.framework.autoexec.constvalue.AutoexecJobPhaseNodeFrom;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
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
     * @param jobVo               作业
     * @param userName            执行用户
     * @param protocolId          协议id
     */
    private boolean updateNodeResourceBySelect(AutoexecJobVo jobVo, AutoexecCombopExecuteConfigVo executeConfigVo, String userName, Long protocolId) {
        List<AutoexecNodeVo> nodeVoList = executeConfigVo.getExecuteNodeConfig().getSelectNodeList();
        boolean isHasNode = false;
        if (CollectionUtils.isNotEmpty(nodeVoList)) {
            ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, null);
            if (Objects.equals(jobVo.getNodeFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())) {
                //如果作业层面的节点则补充前置filter
                AutoexecCombopConfigVo config = jobVo.getConfig();
                if (config != null && config.getExecuteConfig() != null && MapUtils.isNotEmpty(config.getExecuteConfig().getPreCondition())) {
                   searchVo.setPreCondition(JSON.toJavaObject(config.getExecuteConfig().getPreCondition(),ResourceSearchVo.class));
                }
            }
            searchVo.setIdList(nodeVoList.stream().map(AutoexecNodeVo::getId).collect(toList()));
            isHasNode = autoexecJobService.updateNode(jobVo, userName, protocolId, searchVo);
        }
        return isHasNode;
    }


}
