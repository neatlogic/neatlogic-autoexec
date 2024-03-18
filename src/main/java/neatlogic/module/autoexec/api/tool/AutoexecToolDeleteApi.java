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

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecToolDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/delete";
    }

    @Override
    public String getName() {
        return "删除内置工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nameList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "工具名称列表"),
    })
    @Output({
    })
    @Description(desc = "删除内置工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<String> nameList = jsonObj.getJSONArray("nameList").toJavaList(String.class);
        List<AutoexecToolVo> toolList = autoexecToolMapper.getToolByNameList(nameList);
        List<Long> canDeleteToolIdList = new ArrayList<>();
        Map<String, String> canNotDeleteTool = new HashMap<>();
        BasePageVo basePageVo = new BasePageVo();
        basePageVo.setPageSize(100);
        for (AutoexecToolVo toolVo : toolList) {
            List<DependencyInfoVo> dependencyInfoList = DependencyManager.getDependencyList(AutoexecFromType.TOOL, toolVo.getId(), basePageVo);
            if (CollectionUtils.isNotEmpty(dependencyInfoList)) {
                canNotDeleteTool.put(toolVo.getName(), dependencyInfoList.stream().map(DependencyInfoVo::getPath).collect(Collectors.joining("、")));
            } else {
                canDeleteToolIdList.add(toolVo.getId());
            }
//            List<AutoexecCombopVo> referenceList = autoexecToolMapper.getReferenceListByToolId(toolVo.getId());
//            if (referenceList.size() > 0) {
//                canNotDeleteTool.put(toolVo.getName(), referenceList.stream().map(AutoexecCombopVo::getName).collect(Collectors.joining("、")));
//            } else {
//                canDeleteToolIdList.add(toolVo.getId());
//            }
        }
        if (canDeleteToolIdList.size() > 0) {
            autoexecToolMapper.deleteToolByIdList(canDeleteToolIdList);
        }
        if (!canNotDeleteTool.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("以下工具已被组合工具引用，无法删除\n");
            for (Map.Entry<String, String> entry : canNotDeleteTool.entrySet()) {
                sb.append(entry.getKey()).append("(关联的组合工具：").append(entry.getValue()).append(")\n");
            }
            return sb.toString();
        }
        return null;
    }

}
