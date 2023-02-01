/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.service.AuthenticationInfoService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecCombopTypeExecutableApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String getToken() {
        return "autoexec/combop/type/executable/list";
    }

    @Override
    public String getName() {
        return "查询当前用户可执行的组合工具所属分类列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(name = "Return", explode = AutoexecTypeVo[].class, desc = "工具分类列表")
    })
    @Description(desc = "查询当前用户可执行的组合工具所属分类列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AutoexecTypeVo> list;
        if (AuthActionChecker.check(AUTOEXEC_MODIFY.class)) {
            list = autoexecCombopMapper.getAllAutoexecTypeListUsedByCombop();
        } else {
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(UserContext.get().getUserUuid(true));
            Set<Long> idSet = autoexecCombopMapper.getExecutableAutoexecCombopIdListByAuthenticationInfo(authenticationInfoVo);
            list = autoexecCombopMapper.getAutoexecTypeListByCombopIdList(new ArrayList<>(idSet));
        }
        return list;
    }
}
