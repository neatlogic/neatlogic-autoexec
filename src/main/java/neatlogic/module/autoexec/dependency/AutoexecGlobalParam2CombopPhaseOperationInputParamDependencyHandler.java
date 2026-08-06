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
package neatlogic.module.autoexec.dependency;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
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
public class AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler extends DefaultDependencyHandlerBase {

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
                        pathList.add($.t("nmar.dependency.combopprefix") + combopName + ")");
                        pathList.add($.t("nmar.dependency.versionprefix") + autoexecCombopVersionVo.getVersion() + "(" + autoexecCombopVersionVo.getName() + ")");
                        pathList.add($.t("nmar.dependency.phaseprefix") + phaseName + ")");
                        pathList.add($.t("nmar.dependency.operationprefix") + operationName+")");
                        pathList.add($.t("nmar.dependency.inputparammapping"));
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
