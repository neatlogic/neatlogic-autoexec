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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchVisibleAutoexecServiceApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Override
    public String getToken() {
        return "autoexec/service/visible/search";
    }

    @Override
    public String getName() {
        return "nmaa.searchvisibleautoexecserviceapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmaa.common.input.param.desc.keywordmatchname"),
            @Param(name = "parentId", type = ApiParamType.LONG, isRequired = true, desc = "common.parentid"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage")
    })
    @Output({
            @Param(explode= BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecServiceNodeVo[].class, desc = "term.autoexec.servicelist")
    })
    @Description(desc = "nmaa.searchvisibleautoexecserviceapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecServiceSearchVo searchVo = paramObj.toJavaObject(AutoexecServiceSearchVo.class);
        Long parentId = searchVo.getParentId();
        if (parentId != 0L) {
            AutoexecServiceNodeVo parentNodeVo = autoexecServiceMapper.getAutoexecServiceNodeById(parentId);
            if (parentNodeVo == null) {
                throw new AutoexecServiceNotFoundException(parentId);
            }
        }
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        searchVo.setAuthenticationInfoVo(authenticationInfoVo);
        searchVo.setType("service");
        List<AutoexecServiceNodeVo> tbodyList = new ArrayList<>();
        int rowNum = autoexecServiceMapper.getAllVisibleCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            tbodyList = autoexecServiceMapper.getAutoexecServiceNodeVisibleList(searchVo);
            List<Long> idList = tbodyList.stream().map(AutoexecServiceNodeVo::getId).collect(Collectors.toList());
            List<Long> favoriteServiceIdList = autoexecServiceMapper.getFavoriteAutoexecServiceIdListByUserUuidAndServiceIdList(UserContext.get().getUserUuid(true), idList);
            for (AutoexecServiceNodeVo nodeVo : tbodyList) {
                if (favoriteServiceIdList.contains(nodeVo.getId())) {
                    nodeVo.setIsFavorite(1);
                } else {
                    nodeVo.setIsFavorite(0);
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}
