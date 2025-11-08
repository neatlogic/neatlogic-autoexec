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
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.CustomDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2023/1/4 11:21
 */
//@Service
@Deprecated
public class NotifyPolicyAutoexecJobDependencyHandler extends CustomDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    protected String getTableName() {
        return "autoexec_combop";
    }

    @Override
    protected String getFromField() {
        return "notify_policy_id";
    }

    @Override
    protected String getToField() {
        return "id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj instanceof Map) {
            Map<String, Object> map = (Map) dependencyObj;
            Long combopId = (Long) map.get("id");
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
            if (autoexecCombopVo == null) {
                return null;
            }
            String combopName = autoexecCombopVo.getName();
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("combopId", combopId);
            List<String> pathList = new ArrayList<>();
            pathList.add("组合工具");
            pathList.add(combopName);
            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}";
            return new DependencyInfoVo(combopId, dependencyInfoConfig, combopName, pathList, urlFormat, this.getGroupName());
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.NOTIFY_POLICY;
    }
}
