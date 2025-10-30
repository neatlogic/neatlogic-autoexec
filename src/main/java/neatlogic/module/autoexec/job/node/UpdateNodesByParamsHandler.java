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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.job.JobParamNodeNullException;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UpdateNodesByParamsHandler implements IUpdateNodes {
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public boolean update(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        if (CollectionUtils.isEmpty(executeConfigVo.getExecuteNodeConfig().getParamList())) {
            return false;
        }
        return updateNodeResourceByParam(jobVo, executeConfigVo, userName, protocolId);
    }

    /**
     * param
     * 根据运行参数中定义的节点参数 更新作业节点
     *
     * @param executeConfigVo 执行节点配置
     * @param jobVo           作业
     * @param userName        执行用户
     * @param protocolId      协议id
     */
    private boolean updateNodeResourceByParam(AutoexecJobVo jobVo, AutoexecCombopExecuteConfigVo executeConfigVo, String userName, Long protocolId) {
        boolean isHasNode = false;
        List<String> paramList = executeConfigVo.getExecuteNodeConfig().getParamList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            List<AutoexecParamVo> runTimeParamList = jobVo.getRunTimeParamList();
            Set<Long> resourceIdSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(runTimeParamList)) {
                List<ResourceVo> ipPortNameList = new ArrayList<>();
                List<AutoexecParamVo> paramObjList = runTimeParamList.stream().filter(p -> paramList.contains(p.getKey())).collect(Collectors.toList());
                paramObjList.forEach(p -> {
                    if (!(p.getValue() instanceof JSONArray) || CollectionUtils.isEmpty((JSONArray) p.getValue())) {
                        throw new JobParamNodeNullException(p.getKey());
                    }
                    if (p.getValue() instanceof JSONArray) {
                        JSONArray valueArray = (JSONArray) p.getValue();
                        for (int i = 0; i < valueArray.size(); i++) {
                            JSONObject valueObj = valueArray.getJSONObject(i);
                            Long id = valueObj.getLong("id");
                            if (id != null) {
                                resourceIdSet.add(id);
                            } else {
                                String ip = valueObj.getString("ip");
                                if (StringUtils.isNotBlank(ip)) {
                                    Integer port = valueObj.getInteger("port");
                                    String name = valueObj.getString("name");
                                    ipPortNameList.add(new ResourceVo(ip, port, name));
                                }
                            }
                        }
                    }
                });
                ResourceSearchVo searchVo = autoexecJobService.getResourceSearchVoWithCmdbGroupType(jobVo, null, executeConfigVo.getPreCondition());
                if (CollectionUtils.isNotEmpty(ipPortNameList)) {
                    boolean isHasNodeTmp = autoexecJobService.updateNodeByIpPortNameList(ipPortNameList, searchVo, jobVo, userName, protocolId);
                    if (isHasNodeTmp) {
                        isHasNode = true;
                    }
                }
                if (CollectionUtils.isNotEmpty(resourceIdSet)) {
                    boolean isHasNodeTmp = autoexecJobService.updateNodeByResourceIdList(new ArrayList<>(resourceIdSet), searchVo, jobVo, userName, protocolId);
                    if (isHasNodeTmp) {
                        isHasNode = true;
                    }
                }
            }
        }
        return isHasNode;
    }
}
