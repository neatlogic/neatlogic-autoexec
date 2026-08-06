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

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SERVICE_MANAGE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AllAutoexecServiceForTreeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;
    @Override
    public String getToken() {
        return "autoexec/service/all/fortree";
    }

    @Override
    public String getName() {
        return "nmaa.allautoexecservicefortreeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({
            @Param(explode = AutoexecServiceNodeVo[].class, desc = "term.autoexec.servicetreedata")
    })
    @Description(desc = "nmaa.allautoexecservicefortreeapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        int rowNum = autoexecServiceMapper.getAllCount();
        if (rowNum == 0) {
            return new JSONArray();
        }
        AutoexecServiceNodeVo rootNode = new AutoexecServiceNodeVo();
        rootNode.setId(0L);
        rootNode.setName($.t("common.all"));
        rootNode.setParentId(-1L);
        rootNode.setLft(1);
        List<AutoexecServiceNodeVo> allNodeList = new ArrayList<>();
        allNodeList.add(rootNode);
        AutoexecServiceSearchVo searchVo = new AutoexecServiceSearchVo();
        searchVo.setPageSize(100);
        searchVo.setRowNum(rowNum);
        int pageCount = searchVo.getPageCount();
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<AutoexecServiceNodeVo> serviceNodeList = autoexecServiceMapper.getAutoexecServiceNodeList(searchVo);
            allNodeList.addAll(serviceNodeList);
        }
        Map<Long, AutoexecServiceNodeVo> idKeyServiceNodeMap = allNodeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (AutoexecServiceNodeVo nodeVo : allNodeList) {
            AutoexecServiceNodeVo parent = idKeyServiceNodeMap.get(nodeVo.getParentId());
            if (parent != null) {
                parent.addChild(nodeVo);
//                nodeVo.setParent(nodeVo);
            }
        }
        return rootNode.getChildren();
    }
}
