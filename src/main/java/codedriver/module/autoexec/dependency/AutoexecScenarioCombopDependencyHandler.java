/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/10/14 16:37
 */

@Component
public class AutoexecScenarioCombopDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {

        /*暂时前端没有需要依赖跳转，此方法暂时不会被调用*/

        Long combopId = Long.valueOf(dependencyVo.getTo());
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            delete(combopId);
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

        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("combopId", combopId);
        List<String> pathList = new ArrayList<>();
        pathList.add("组合工具");
        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}";
        return new DependencyInfoVo(Long.valueOf(dependencyVo.getTo()), dependencyInfoConfig, autoexecCombopVo.getName(), pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCENARIO;
    }
}
