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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.CombopAuthorityAction;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopSearchVo;
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
import java.util.*;

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
        AutoexecCombopSearchVo searchVo = jsonObj.toJavaObject(AutoexecCombopSearchVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            autoexecCombopList = autoexecCombopMapper.getAutoexecCombopByIdList(idList);
            searchVo.setRowNum(autoexecCombopList.size());
            return TableResultUtil.getResult(autoexecCombopList, searchVo);
        }
        searchVo.setUserUuid(UserContext.get().getUserUuid());
        List<String> uuidList = new ArrayList<>();
        uuidList.add(UserContext.get().getUserUuid());
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        if (CollectionUtils.isNotEmpty(authenticationInfoVo.getTeamUuidList())) {
            uuidList.addAll(authenticationInfoVo.getTeamUuidList());
        }
        if (CollectionUtils.isNotEmpty(authenticationInfoVo.getRoleUuidList())) {
            uuidList.addAll(authenticationInfoVo.getRoleUuidList());
        }
        searchVo.setUuidList(uuidList);
        if (AuthActionChecker.check(AUTOEXEC_MODIFY.class)) {
            searchVo.setIsHasAllAuthority(1);
        } else {
            searchVo.setIsHasAllAuthority(0);
        }
        searchVo.setVersionStatus(ScriptVersionStatus.PASSED.getValue());
        searchVo.setAction(CombopAuthorityAction.EXECUTE.getValue());
        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> combopIdList = autoexecCombopMapper.getAutoexecCombopIdList(searchVo);
            if (CollectionUtils.isNotEmpty(combopIdList)) {
                autoexecCombopList = autoexecCombopMapper.getAutoexecCombopByIdList(combopIdList);
            }
        }
        return TableResultUtil.getResult(autoexecCombopList, searchVo);
    }
}
