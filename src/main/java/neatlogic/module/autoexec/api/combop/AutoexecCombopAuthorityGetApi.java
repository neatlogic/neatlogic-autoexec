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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopAuthorityAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopAuthorityVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询组合工具授权信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
//@Service
@Deprecated
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopAuthorityGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/authority/get";
    }

    @Override
    public String getName() {
        return "查询组合工具授权信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, desc = "编辑授权列表"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, desc = "执行授权列表")
    })
    @Description(desc = "查询组合工具授权信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        JSONObject resultObj = new JSONObject();
        for (CombopAuthorityAction authorityAction : CombopAuthorityAction.values()) {
            List<String> authorityList = new ArrayList<>();
            List<AutoexecCombopAuthorityVo> authorityVoList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndAction(combopId, authorityAction.getValue());
            for (AutoexecCombopAuthorityVo autoexecCombopAuthorityVo : authorityVoList) {
                authorityList.add(autoexecCombopAuthorityVo.getType() + "#" + autoexecCombopAuthorityVo.getUuid());
            }
            resultObj.put(authorityAction.getValue() + "AuthorityList", authorityList);
        }
        return resultObj;
    }
}
