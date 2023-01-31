/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.autoexec.dependency;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 组合工具阶段操作自由参数映射引用全局参数处理器
 */
@Component
public class AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long combopId = config.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            return null;
        }
        AutoexecCombopConfigVo combopConfigVo = autoexecCombopVo.getConfig();
        if (combopConfigVo == null) {
            return null;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = combopConfigVo.getCombopPhaseList();
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
                List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
                if (CollectionUtils.isEmpty(argumentMappingList)) {
                    return null;
                }
                for (ParamMappingVo paramMappingVo : argumentMappingList) {
                    if (paramMappingVo == null) {
                        continue;
                    }
                    if (!Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        continue;
                    }
                    if (Objects.equals(paramMappingVo.getValue(), dependencyVo.getFrom())) {
                        String operationName = phaseOperationVo.getOperationName();
                        String phaseName = combopPhaseVo.getName();
                        String combopName = autoexecCombopVo.getName();
                        JSONObject dependencyInfoConfig = new JSONObject();
                        dependencyInfoConfig.put("combopId", combopId);
                        List<String> pathList = new ArrayList<>();
                        pathList.add("组合工具");
                        pathList.add(combopName);
                        pathList.add(phaseName);
                        pathList.add(operationName);
                        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}";
                        String value = id + "_" + System.currentTimeMillis();
                        return new DependencyInfoVo(value, dependencyInfoConfig, "自由参数", pathList, urlFormat, this.getGroupName());
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