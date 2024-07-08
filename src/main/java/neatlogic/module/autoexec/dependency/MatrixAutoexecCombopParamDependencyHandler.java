/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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
 * 组合工具参数引用矩阵关系处理器
 * @author linbq
 * @since 2021/6/21 16:31
 **/
//@Service
@Deprecated
public class MatrixAutoexecCombopParamDependencyHandler extends CustomDependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    protected String getTableName() {
        return "autoexec_combop_param_matrix";
    }

    @Override
    protected String getFromField() {
        return "matrix_uuid";
    }

    @Override
    protected String getToField() {
        return "combop_id";
    }

    @Override
    protected List<String> getToFieldList() {
        List<String> result = new ArrayList<>();
        result.add("combop_id");
        result.add("key");
        return result;
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj == null) {
            return null;
        }
        if(dependencyObj instanceof Map){
            Map<String, Object> map = (Map) dependencyObj;
            Long combopId = (Long) map.get("combop_id");
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
            if (autoexecCombopVo != null) {
                String key = (String) map.get("key");
//                AutoexecCombopParamVo autoexecCombopParamVo = autoexecCombopMapper.getAutoexecCombopParamByCombopIdAndKey(autoexecCombopVo.getId(), key);
//                if (autoexecCombopParamVo != null) {
//                    JSONObject dependencyInfoConfig = new JSONObject();
//                    dependencyInfoConfig.put("combopId", autoexecCombopVo.getId());
////                    dependencyInfoConfig.put("combopName", autoexecCombopVo.getName());
////                    dependencyInfoConfig.put("paramName", autoexecCombopParamVo.getName());
//                    List<String> pathList = new ArrayList<>();
//                    pathList.add("组合工具");
//                    pathList.add(autoexecCombopVo.getName());
//                    pathList.add("作业参数");
//                    String lastName = autoexecCombopParamVo.getName();
////                    String pathFormat = "组合工具-${DATA.combopName}-运行参数-${DATA.paramName}";
//                    String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/action-detail?id=${DATA.combopId}";
//                    return new DependencyInfoVo(autoexecCombopVo.getId(), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
//                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.MATRIX;
    }
}
