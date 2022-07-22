/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AutoexecScript2CombopPhaseOperationDependencyHandler extends FixedTableDependencyHandlerBase {

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
                if (!Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                    return null;
                }
                if (!Objects.equals(phaseOperationVo.getOperationId().toString(), dependencyVo.getFrom())) {
                    return null;
                }
                String operationName = phaseOperationVo.getOperationName();
                String phaseName = combopPhaseVo.getName();
                String combopName = autoexecCombopVo.getName();
                JSONObject dependencyInfoConfig = new JSONObject();
                dependencyInfoConfig.put("combopId", combopId);
                List<String> pathList = new ArrayList<>();
                pathList.add("组合工具");
                pathList.add(combopName);
                pathList.add(phaseName);
                String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}";
                return new DependencyInfoVo(id, dependencyInfoConfig, operationName, pathList, urlFormat, this.getGroupName());
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCRIPT;
    }
}
