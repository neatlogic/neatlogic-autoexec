/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.dependency;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
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
public class AutoexecCombop2AutoexecServiceDependencyHandler extends FixedTableDependencyHandlerBase {

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
        pathList.add("服务目录管理");
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
