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
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class Notify2AutoexecCombopDependencyHandler extends DefaultDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {

        Long combopId = Long.valueOf(dependencyVo.getTo());
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            return null;
        }
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            return null;
        }

        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("combopId", combopId);
        List<String> pathList = new ArrayList<>();
        pathList.add($.t("term.autoexec.combop"));
        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}&versionStatus=passed";
        return new DependencyInfoVo(Long.valueOf(dependencyVo.getTo()), dependencyInfoConfig, autoexecCombopVo.getName(), pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.NOTIFY_POLICY;
    }
}
