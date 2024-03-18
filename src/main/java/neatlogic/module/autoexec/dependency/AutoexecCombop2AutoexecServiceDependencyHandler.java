/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
