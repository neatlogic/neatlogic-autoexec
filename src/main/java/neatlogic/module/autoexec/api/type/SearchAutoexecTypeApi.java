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

package neatlogic.module.autoexec.api.type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAutoexecTypeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/search";
    }

    @Override
    public String getName() {
        return "nmaa.searchautoexectypeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmaa.common.input.param.desc.needpage"),
            @Param(name = "isNeedCheckDataAuth", type = ApiParamType.INTEGER, desc = "nmaa.common.input.param.desc.isneedcheckdataauth")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecTypeVo[].class, desc = "nmaa.searchautoexectypeapi.output.param.desc.tbodylist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmaa.searchautoexectypeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecTypeVo typeVo = JSON.toJavaObject(jsonObj, AutoexecTypeVo.class);
        List<AutoexecTypeVo> typeList = new ArrayList<>();
        List<Long> typeIdList = autoexecTypeMapper.searchTypeIdList(typeVo);
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            List<AutoexecTypeVo> list = autoexecTypeMapper.getTypeListByIdList(typeIdList);
            for (Long id : typeIdList) {
                for (AutoexecTypeVo autoexecTypeVo : list) {
                    if (Objects.equals(autoexecTypeVo.getId(), id)) {
                        typeList.add(autoexecTypeVo);
                        break;
                    }
                }
            }
        }
        result.put("tbodyList", typeList);
        if (CollectionUtils.isNotEmpty(typeList)) {
            List<Long> idList = typeList.stream().map(AutoexecTypeVo::getId).collect(Collectors.toList());
            List<AutoexecTypeVo> referenceCountListForTool = autoexecTypeMapper.getReferenceCountListForTool(idList);
            List<AutoexecTypeVo> referenceCountListForScript = autoexecTypeMapper.getReferenceCountListForScript(idList);
            List<AutoexecTypeVo> referenceCountListForCombop = autoexecTypeMapper.getReferenceCountListForCombop(idList);
            Map<Long, Integer> referenceCountForToolMap = new HashMap<>();
            Map<Long, Integer> referenceCountForScriptMap = new HashMap<>();
            Map<Long, Integer> referenceCountForCombopMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(referenceCountListForTool)) {
                referenceCountForToolMap = referenceCountListForTool.stream()
                        .collect(Collectors.toMap(AutoexecTypeVo::getId, AutoexecTypeVo::getReferenceCountForTool));
            }
            if (CollectionUtils.isNotEmpty(referenceCountListForScript)) {
                referenceCountForScriptMap = referenceCountListForScript.stream()
                        .collect(Collectors.toMap(AutoexecTypeVo::getId, AutoexecTypeVo::getReferenceCountForScript));
            }
            if (CollectionUtils.isNotEmpty(referenceCountListForCombop)) {
                referenceCountForCombopMap = referenceCountListForCombop.stream()
                        .collect(Collectors.toMap(AutoexecTypeVo::getId, AutoexecTypeVo::getReferenceCountForCombop));
            }
            for (AutoexecTypeVo vo : typeList) {
                Integer referenceCountForTool = referenceCountForToolMap.get(vo.getId());
                Integer referenceCountForScript = referenceCountForScriptMap.get(vo.getId());
                Integer referenceCountForCombop = referenceCountForCombopMap.get(vo.getId());
                vo.setReferenceCountForTool(referenceCountForTool != null ? referenceCountForTool : 0);
                vo.setReferenceCountForScript(referenceCountForScript != null ? referenceCountForScript : 0);
                vo.setReferenceCountForCombop(referenceCountForCombop != null ? referenceCountForCombop : 0);
            }
        }
        if (typeVo.getNeedPage()) {
            int rowNum = autoexecTypeMapper.searchTypeCount(typeVo);
            typeVo.setRowNum(rowNum);
            result.put("currentPage", typeVo.getCurrentPage());
            result.put("pageSize", typeVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, typeVo.getPageSize()));
            result.put("rowNum", typeVo.getRowNum());
        }
        return result;
    }


}
