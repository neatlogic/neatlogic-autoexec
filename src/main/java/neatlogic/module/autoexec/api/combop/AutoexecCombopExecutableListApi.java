/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 查询当前用户可执行的组合工具列表接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopExecutableListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/executable/list";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccombopexecutablelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "common.typeid"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecCombopVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmaac.autoexeccombopexecutablelistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AutoexecCombopVo> autoexecCombopList = new ArrayList<>();
        AutoexecCombopVo searchVo = JSONObject.toJavaObject(jsonObj, AutoexecCombopVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            autoexecCombopList = autoexecCombopMapper.getAutoexecCombopListByIdList(idList);
            searchVo.setRowNum(autoexecCombopList.size());
            return TableResultUtil.getResult(autoexecCombopList, searchVo);
        }
        if (AuthActionChecker.check(AUTOEXEC_MODIFY.class)) {
            //用户拥有“自动化管理员权限”时，对所有的组合工具都有执行权限
            searchVo.setIsActive(1);
            int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                autoexecCombopList = autoexecCombopMapper.getAutoexecCombopList(searchVo);
            }
        } else {
            //用户没有“自动化管理员权限”时，组合工具维护人或者有执行授权
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            Set<Long> idSet = autoexecCombopMapper.getExecutableAutoexecCombopIdListByKeywordAndAuthenticationInfo(searchVo.getKeyword(), searchVo.getTypeId(), authenticationInfoVo);
            List<Long> idList = new ArrayList<>(idSet);
            idList.sort(Comparator.reverseOrder());
            int rowNum = idList.size();
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                int fromIndex = searchVo.getStartNum();
                int toIndex = fromIndex + searchVo.getPageSize();
                toIndex = toIndex > rowNum ? rowNum : toIndex;
                List<Long> currentPageIdList = idList.subList(fromIndex, toIndex);
                if (CollectionUtils.isNotEmpty(currentPageIdList)) {
                    autoexecCombopList = autoexecCombopMapper.getAutoexecCombopListByIdList(currentPageIdList);
                }
            }
        }
        return TableResultUtil.getResult(autoexecCombopList, searchVo);
    }
}
