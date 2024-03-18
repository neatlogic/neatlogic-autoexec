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

package neatlogic.module.autoexec.api.customtemplate;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCustomTemplateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;


    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = CustomTemplateVo[].class)
    })
    @Description(desc = "查询自定义模板接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CustomTemplateVo customTemplateVo = JSONObject.toJavaObject(jsonObj, CustomTemplateVo.class);
        int rowNum = autoexecCustomTemplateMapper.searchCustomTemplateCount(customTemplateVo);
        List<CustomTemplateVo> customTemplateList = null;
        if (rowNum > 0) {
            customTemplateVo.setRowNum(rowNum);
            customTemplateList = autoexecCustomTemplateMapper.searchCustomTemplate((customTemplateVo));
            if (customTemplateList.size() > 0) {
                List<Long> idList = customTemplateList.stream().map(CustomTemplateVo::getId).collect(Collectors.toList());
                Map<Long, Integer> referenceCountForTool = autoexecCustomTemplateMapper.getReferenceCountListForTool(idList).stream().collect(Collectors.toMap(CustomTemplateVo::getId, CustomTemplateVo::getReferenceCountForTool));
                Map<Long, Integer> referenceCountForScript = autoexecCustomTemplateMapper.getReferenceCountListForScript(idList).stream().collect(Collectors.toMap(CustomTemplateVo::getId, CustomTemplateVo::getReferenceCountForScript));
                for (CustomTemplateVo vo : customTemplateList) {
                    if (MapUtils.isNotEmpty(referenceCountForTool)) {
                        Integer count = referenceCountForTool.get(vo.getId());
                        vo.setReferenceCountForTool(count != null ? count : 0);
                    }
                    if (MapUtils.isNotEmpty(referenceCountForScript)) {
                        Integer count = referenceCountForScript.get(vo.getId());
                        vo.setReferenceCountForScript(count != null ? count : 0);
                    }
                }
            }
        }
        return TableResultUtil.getResult(customTemplateList, customTemplateVo);
    }


    @Override
    public String getName() {
        return "查询自定义模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/customtemplate/search";
    }
}
