/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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

    @Override
    public String getToken() {
        return "autoexec/combop/type/executable/list";
    }

    @Override
    public String getName() {
        return "nmaac.listautoexeccomboptypeexecutableapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(name = "Return", explode = AutoexecTypeVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmaac.listautoexeccomboptypeexecutableapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AutoexecTypeVo> list;
        if (AuthActionChecker.check(AUTOEXEC_MODIFY.class)) {
            list = autoexecCombopMapper.getAllAutoexecTypeListUsedByCombop();
        } else {
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            Set<Long> idSet = autoexecCombopMapper.getExecutableAutoexecCombopIdListByAuthenticationInfo(authenticationInfoVo);
            list = autoexecCombopMapper.getAutoexecTypeListByCombopIdList(new ArrayList<>(idSet));
        }
        return list;
    }
}
