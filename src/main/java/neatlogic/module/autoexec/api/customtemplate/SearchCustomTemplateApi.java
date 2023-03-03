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

package neatlogic.module.autoexec.api.customtemplate;

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
import com.alibaba.fastjson.JSONObject;
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
