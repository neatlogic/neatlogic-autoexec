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
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 组合工具参数引用矩阵关系处理器
 * @author linbq
 * @since 2021/6/21 16:31
 **/
@Service
public class Matrix2AutoexecCombopVersionParamDependencyHandler extends DefaultDependencyHandlerBase {

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
        List<AutoexecParamVo> runtimeParamList = versionConfigVo.getRuntimeParamList();
        if (CollectionUtils.isEmpty(runtimeParamList)) {
            return null;
        }
        for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
            if (Objects.equals(dependencyVo.getTo(), autoexecParamVo.getId().toString())) {
                AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
                if (autoexecCombopVo == null) {
                    return null;
                }
                JSONObject dependencyInfoConfig = new JSONObject();
                dependencyInfoConfig.put("combopId", autoexecCombopVo.getId());
                dependencyInfoConfig.put("versionId", autoexecCombopVersionVo.getId());
                List<String> pathList = new ArrayList<>();
                pathList.add($.t("nmar.dependency.combopprefix") + autoexecCombopVo.getName() + ")");
                pathList.add($.t("nmar.dependency.versionprefix") + autoexecCombopVersionVo.getVersion() + "(" + autoexecCombopVersionVo.getName() + ")");
                pathList.add($.t("term.autoexec.jobparam"));
                String lastName = autoexecParamVo.getName();
                String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}&versionId=${DATA.versionId}";
                return new DependencyInfoVo(autoexecParamVo.getId(), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.MATRIX;
    }
}
