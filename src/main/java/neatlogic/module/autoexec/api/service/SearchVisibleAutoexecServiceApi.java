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
        return "搜索可见服务目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字，匹配名称"),
            @Param(name = "parentId", type = ApiParamType.LONG, isRequired = true, desc = "父级ID"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
            @Param(explode= BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecServiceNodeVo[].class, desc = "服务目录列表")
    })
    @Description(desc = "搜索服务目录")
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
