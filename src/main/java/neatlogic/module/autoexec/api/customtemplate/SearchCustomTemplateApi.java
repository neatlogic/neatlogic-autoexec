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
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = CustomTemplateVo[].class)
    })
    @Description(desc = "nmaa.searchcustomtemplateapi.getname")
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
        return "nmaa.searchcustomtemplateapi.getname";
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
