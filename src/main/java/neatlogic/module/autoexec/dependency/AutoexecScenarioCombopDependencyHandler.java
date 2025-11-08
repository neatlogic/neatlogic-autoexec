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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/10/14 16:37
 */

@Component
public class AutoexecScenarioCombopDependencyHandler extends DefaultDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {

        /*暂时前端没有需要依赖跳转，此方法暂时不会被调用*/

        Long versionId = Long.valueOf(dependencyVo.getTo());
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
        if (autoexecCombopVersionVo == null) {
            return null;
        }
        AutoexecCombopVersionConfigVo versionConfigVo = autoexecCombopVersionVo.getConfig();
        if (versionConfigVo == null) {
            return null;
        }
        Long combopId = autoexecCombopVersionVo.getCombopId();
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            return null;
        }
        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("combopId", combopId);
        dependencyInfoConfig.put("versionId", versionId);
        List<String> pathList = new ArrayList<>();
        pathList.add("组合工具(" + autoexecCombopVo.getName() + ")");
        String lastName = "版本" + autoexecCombopVersionVo.getVersion() + "(" + autoexecCombopVersionVo.getName() + ")";
        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}&versionId=${DATA.versionId}";
        return new DependencyInfoVo(Long.valueOf(dependencyVo.getTo()), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCENARIO;
    }
}
