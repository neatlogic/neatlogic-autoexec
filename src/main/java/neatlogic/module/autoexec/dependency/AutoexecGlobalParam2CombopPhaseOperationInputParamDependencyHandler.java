/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package neatlogic.module.autoexec.dependency;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 * 组合工具阶段操作输入参数映射引用全局参数处理器
 */
@Component
public class AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long versionId = config.getLong("versionId");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
        if (autoexecCombopVersionVo == null) {
            return null;
        }
        AutoexecCombopVersionConfigVo versionConfigVo = autoexecCombopVersionVo.getConfig();
        if (versionConfigVo == null) {
            return null;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = versionConfigVo.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return null;
        }
        Long phaseId = config.getLong("phaseId");
        for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (!Objects.equals(combopPhaseVo.getId(), phaseId)) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
            if (phaseConfigVo == null) {
                return null;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfigVo.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                return null;
            }
            Long id = Long.valueOf(dependencyVo.getTo());
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                if (!Objects.equals(phaseOperationVo.getId(), id)) {
                    continue;
                }
                AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
                if (operationConfigVo == null) {
                    return null;
                }
                List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
                if (CollectionUtils.isEmpty(paramMappingList)) {
                    return null;
                }
                for (ParamMappingVo paramMappingVo : paramMappingList) {
                    if (paramMappingVo == null) {
                        continue;
                    }
                    if (!Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        continue;
                    }
                    if (Objects.equals(paramMappingVo.getValue(), dependencyVo.getFrom())) {
                        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
                        if (autoexecCombopVo == null) {
                            return null;
                        }
                        String operationName = phaseOperationVo.getOperationName();
                        String phaseName = combopPhaseVo.getName();
                        String combopName = autoexecCombopVo.getName();
                        String key = config.getString("key");
                        String name = paramMappingVo.getName();
                        if (StringUtils.isBlank(name)) {
                            name = config.getString("name");
                            if (StringUtils.isBlank(name)) {
                                name = key;
                            }
                        }
                        JSONObject dependencyInfoConfig = new JSONObject();
                        dependencyInfoConfig.put("combopId", autoexecCombopVo.getId());
                        dependencyInfoConfig.put("versionId", autoexecCombopVersionVo.getId());
                        List<String> pathList = new ArrayList<>();
                        pathList.add("组合工具(" + combopName + ")");
                        pathList.add("版本" + autoexecCombopVersionVo.getVersion() + "(" + autoexecCombopVersionVo.getName() + ")");
                        pathList.add("阶段(" + phaseName + ")");
                        pathList.add("操作(" + operationName+")");
                        pathList.add("输入参数映射");
                        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}&versionId=${DATA.versionId}";
                        String value = id + "_" + key;
                        return new DependencyInfoVo(value, dependencyInfoConfig, name, pathList, urlFormat, this.getGroupName());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.GLOBAL_PARAM;
    }
}
