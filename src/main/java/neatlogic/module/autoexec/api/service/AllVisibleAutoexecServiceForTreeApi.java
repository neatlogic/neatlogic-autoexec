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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecServiceType;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AllVisibleAutoexecServiceForTreeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Override
    public String getToken() {
        return "autoexec/service/all/visible/fortree";
    }

    @Override
    public String getName() {
        return "获取当前用户可见的服务目录树结构数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "needService", type = ApiParamType.BOOLEAN, defaultValue = "false", desc = "是否需要服务节点数据")
    })
    @Output({
            @Param(explode = AutoexecServiceNodeVo[].class, desc = "服务目录树结构数据")
    })
    @Description(desc = "获取当前用户可见的服务目录树结构数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecServiceSearchVo searchVo = new AutoexecServiceSearchVo();
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
        Map<Long, AutoexecServiceNodeVo> idKeyServiceNodeMap = allNodeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        // 遍历所有节点，建立父子关系，并找出所有叶子节点
        List<AutoexecServiceNodeVo> leafNodeList = new ArrayList<>();
        for (AutoexecServiceNodeVo nodeVo : allNodeList) {
            AutoexecServiceNodeVo parent = idKeyServiceNodeMap.get(nodeVo.getParentId());
            if (parent != null) {
                parent.addChild(nodeVo);
                nodeVo.setParent(parent);
            }
            if (Objects.equals(nodeVo.getType(), AutoexecServiceType.SERVICE.getValue())) {
                leafNodeList.add(nodeVo);
            }
        }
        // 遍历所有叶子节点，找出叶子节点所有上游节点ID
        List<Long> visibleCatalogNodeIdList = new ArrayList<>();
        for (AutoexecServiceNodeVo nodeVo : leafNodeList) {
            AutoexecServiceNodeVo tempNodeVo = nodeVo;
            while (true) {
                AutoexecServiceNodeVo parent = tempNodeVo.getParent();
                if (parent == null) {
                    break;
                }
                if (visibleCatalogNodeIdList.contains(parent.getId())) {
                    break;
                }
                visibleCatalogNodeIdList.add(parent.getId());
                tempNodeVo = parent;
            }
        }
        Boolean needService = paramObj.getBoolean("needService");
        if (!Objects.equals(needService, true)) {
            // 遍历所有叶子节点，并将叶子节点从树中移除
            for (AutoexecServiceNodeVo nodeVo : leafNodeList) {
                AutoexecServiceNodeVo parent = nodeVo.getParent();
                if (parent != null) {
                    nodeVo.setParent(null);
                    parent.removeChild(nodeVo);
                }
            }
        }
        // 遍历所有节点，把节点id不在visibleNodeIdList中的节点从树中移除
        for (AutoexecServiceNodeVo nodeVo : allNodeList) {
            if (Objects.equals(nodeVo.getType(), AutoexecServiceType.CATALOG.getValue()) && !visibleCatalogNodeIdList.contains(nodeVo.getId())) {
                AutoexecServiceNodeVo parent = nodeVo.getParent();
                if (parent != null) {
                    nodeVo.setParent(null);
                    parent.removeChild(nodeVo);
                }
            }
        }
        List<AutoexecServiceNodeVo> result = rootNode.getChildren();
        rootNode.setChildren(null);
        result.add(rootNode);
        return result;
    }
}
