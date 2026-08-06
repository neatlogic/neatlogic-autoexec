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
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class AutoexecCombop2AutoexecServiceDependencyHandler extends DefaultDependencyHandlerBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        Long toId = Long.valueOf(dependencyVo.getTo());
        AutoexecServiceNodeVo autoexecServiceNodeVo = autoexecServiceMapper.getAutoexecServiceNodeById(toId);
        if (autoexecServiceNodeVo == null) {
            return null;
        }
        List<String> upwardNameList = autoexecServiceMapper.getUpwardNameListByLftAndRht(autoexecServiceNodeVo.getLft(), autoexecServiceNodeVo.getRht());
        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("id", autoexecServiceNodeVo.getId());
        List<String> pathList = new ArrayList<>();
        pathList.add($.t("nmar.dependency.servicecatalogmanagement"));
        if (CollectionUtils.isNotEmpty(upwardNameList)) {
            upwardNameList.remove(upwardNameList.size() - 1);
            pathList.addAll(upwardNameList);
        }
        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/catalog-manage?id=${DATA.id}";
        return new DependencyInfoVo(toId, dependencyInfoConfig, autoexecServiceNodeVo.getName(), pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.COMBOP;
    }
}
