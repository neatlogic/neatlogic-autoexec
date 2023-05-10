/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.dependency;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.CustomTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

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
public class MatrixAutoexecCombopParamDependencyHandler extends CustomTableDependencyHandlerBase {

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
