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
        return "获取所有服务目录树结构数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({
            @Param(explode = AutoexecServiceNodeVo[].class, desc = "服务目录树结构数据")
    })
    @Description(desc = "获取所有服务目录树结构数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        int rowNum = autoexecServiceMapper.getAllCount();
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
