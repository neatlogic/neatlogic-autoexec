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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceBreadcrumbVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAutoexecServiceBreadcrumbApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Override
    public String getToken() {
        return "autoexec/service/breadcrumb/search";
    }

    @Override
    public String getName() {
        return "搜索某个服务目录下的当前用户可见的服务目录路径列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字，匹配名称"),
            @Param(name = "parentId", type = ApiParamType.LONG, defaultValue = "0", desc = "父级ID"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
            @Param(explode= BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecServiceBreadcrumbVo[].class, desc = "服务目录路径列表")
    })
    @Description(desc = "搜索某个服务目录下的当前用户可见的服务目录路径列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecServiceSearchVo searchVo = paramObj.toJavaObject(AutoexecServiceSearchVo.class);
        Long parentId = searchVo.getParentId();
        if (parentId != null && parentId != 0L) {
            AutoexecServiceNodeVo parentNodeVo = autoexecServiceMapper.getAutoexecServiceNodeById(parentId);
            if (parentNodeVo == null) {
                throw new AutoexecServiceNotFoundException(parentId);
            }
            searchVo.setLft(parentNodeVo.getLft());
            searchVo.setRht(parentNodeVo.getRht());
        }
        searchVo.setParentId(null);
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        searchVo.setAuthenticationInfoVo(authenticationInfoVo);
        int rowNum = autoexecServiceMapper.getAllVisibleCount(searchVo);
        if (rowNum == 0) {
            return new JSONArray();
        }
        AutoexecServiceNodeVo rootNode = new AutoexecServiceNodeVo();
        rootNode.setId(0L);
        rootNode.setName("所有");
        rootNode.setParentId(-1L);
        rootNode.setLft(1);
        List<AutoexecServiceNodeVo> allNodeList = new ArrayList<>();
        allNodeList.add(rootNode);
        searchVo.setPageSize(100);
        searchVo.setRowNum(rowNum);
        int pageCount = searchVo.getPageCount();
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<AutoexecServiceNodeVo> serviceNodeList = autoexecServiceMapper.getAutoexecServiceNodeVisibleList(searchVo);
            allNodeList.addAll(serviceNodeList);
        }
        allNodeList.sort(Comparator.comparingInt(AutoexecServiceNodeVo::getLft));
        Map<Long, AutoexecServiceNodeVo> idKeyServiceNodeMap = allNodeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        // 遍历所有节点，建立父子关系，并找出所有叶子节点
        List<AutoexecServiceNodeVo> leafNodeList = new ArrayList<>();
        for (AutoexecServiceNodeVo nodeVo : allNodeList) {
            AutoexecServiceNodeVo parent = idKeyServiceNodeMap.get(nodeVo.getParentId());
            if (parent != null) {
                nodeVo.setParent(parent);
            }
            if (Objects.equals(nodeVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
                leafNodeList.add(nodeVo);
            }
        }
        // 遍历所有叶子节点，找出叶子节点路径，并将叶子节点从树中移除
        Map<Long, AutoexecServiceBreadcrumbVo> breadcrumbMap = new HashMap<>();
        List<AutoexecServiceBreadcrumbVo> breadcrumbList = new ArrayList<>();
        for (AutoexecServiceNodeVo nodeVo : leafNodeList) {
            AutoexecServiceNodeVo tempNodeVo = nodeVo;
            AutoexecServiceNodeVo parent = tempNodeVo.getParent();
            if (parent == null) {
                continue;
            }
            AutoexecServiceBreadcrumbVo breadcrumbVo = breadcrumbMap.get(parent.getId());
            if (breadcrumbVo != null) {
                breadcrumbVo.addTbody(nodeVo);
                continue;
            }
            breadcrumbVo = new AutoexecServiceBreadcrumbVo();
            breadcrumbVo.addTbody(nodeVo);
            breadcrumbMap.put(parent.getId(), breadcrumbVo);
            breadcrumbList.add(breadcrumbVo);
            breadcrumbVo.setId(parent.getId());
            breadcrumbVo.setLft(parent.getLft());
            breadcrumbVo.addUpwardName(parent.getName());
            tempNodeVo = parent;
            while(true) {
                parent = tempNodeVo.getParent();
                if (parent == null) {
                    break;
                }
                if (parent.getId() == 0) {
                    break;
                }
                breadcrumbVo.addUpwardName(parent.getName());
                tempNodeVo = parent;
            }
            nodeVo.setParent(null);
        }
        breadcrumbList.sort(Comparator.comparingInt(AutoexecServiceBreadcrumbVo::getLft));
        List<Long> serviceIdList = new ArrayList<>();
        searchVo = paramObj.toJavaObject(AutoexecServiceSearchVo.class);
        List<AutoexecServiceBreadcrumbVo> finalBreadcrumbList = PageUtil.subList(breadcrumbList, searchVo);
        for (AutoexecServiceBreadcrumbVo breadcrumbVo : finalBreadcrumbList) {
            breadcrumbVo.setPageSize(searchVo.getPageSize());
            Collections.reverse(breadcrumbVo.getUpwardNameList());
            List<AutoexecServiceVo> tbodyList = PageUtil.subList(breadcrumbVo.getTbodyList(), breadcrumbVo);
            breadcrumbVo.setTbodyList(tbodyList);
            serviceIdList.addAll(tbodyList.stream().map(AutoexecServiceVo::getId).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(serviceIdList)) {
            List<Long> favoriteServiceIdList = autoexecServiceMapper.getFavoriteAutoexecServiceIdListByUserUuidAndServiceIdList(UserContext.get().getUserUuid(true), serviceIdList);
            for (AutoexecServiceBreadcrumbVo breadcrumbVo : finalBreadcrumbList) {
                for (AutoexecServiceVo serviceVo : breadcrumbVo.getTbodyList()) {
                    if (favoriteServiceIdList.contains(serviceVo.getId())) {
                        serviceVo.setIsFavorite(1);
                    } else {
                        serviceVo.setIsFavorite(0);
                    }
                }
            }
        }
        return TableResultUtil.getResult(finalBreadcrumbList, searchVo);
    }
}
