/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String getToken() {
        return "autoexec/combop/executable/list";
    }

    @Override
    public String getName() {
        return "查询当前用户可执行的组合工具列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询，支持名称或唯一标识"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecCombopVo[].class, desc = "组合工具列表")
    })
    @Description(desc = "查询当前用户可执行的组合工具列表")
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
            int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                autoexecCombopList = autoexecCombopMapper.getAutoexecCombopList(searchVo);
            }
        } else {
            //用户没有“自动化管理员权限”时，组合工具维护人或者有执行授权
            String userUuid = UserContext.get().getUserUuid(true);
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(userUuid);
            Set<Long> idSet = autoexecCombopMapper.getExecutableAutoexecCombopIdListByKeywordAndAuthenticationInfo(searchVo.getKeyword(), searchVo.getTypeId(), authenticationInfoVo);
            List<Long> idList = new ArrayList<>(idSet);
            idList.sort(Comparator.reverseOrder());
            int rowNum = idList.size();
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                int fromIndex = searchVo.getStartNum();
                int toIndex = fromIndex + searchVo.getPageSize();
                toIndex = toIndex >  rowNum ? rowNum : toIndex;
                List<Long> currentPageIdList =  idList.subList(fromIndex, toIndex);
                if (CollectionUtils.isNotEmpty(currentPageIdList)) {
                    autoexecCombopList = autoexecCombopMapper.getAutoexecCombopListByIdList(currentPageIdList);
                }
            }
        }

        return TableResultUtil.getResult(autoexecCombopList, searchVo);
    }
}
