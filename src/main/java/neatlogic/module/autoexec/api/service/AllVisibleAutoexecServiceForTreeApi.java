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
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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

    @Input({})
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
        // 遍历所有叶子节点，找出叶子节点所有上游节点ID，并将叶子节点从树中移除
        List<Long> visibleNodeIdList = new ArrayList<>();
        for (AutoexecServiceNodeVo nodeVo : leafNodeList) {
            AutoexecServiceNodeVo tempNodeVo = nodeVo;
            while(true) {
                AutoexecServiceNodeVo parent = tempNodeVo.getParent();
                if (parent == null) {
                    break;
                }
                if (visibleNodeIdList.contains(parent.getId())) {
                    break;
                }
                visibleNodeIdList.add(parent.getId());
                tempNodeVo = parent;
            }
            AutoexecServiceNodeVo parent = nodeVo.getParent();
            if (parent != null) {
                nodeVo.setParent(null);
                parent.removeChild(nodeVo);
            }
        }
        // 遍历所有节点，把节点id不在visibleNodeIdList中的节点从树中移除
        for (AutoexecServiceNodeVo nodeVo : allNodeList) {
            if (!visibleNodeIdList.contains(nodeVo.getId())) {
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
