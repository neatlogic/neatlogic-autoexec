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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class AutoexecCombopVersionGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/version/get";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccombopversiongetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id")
    })
    @Output({
            @Param(explode = AutoexecCombopVersionVo.class, desc = "term.autoexec.combopdetailsinfo")
    })
    @Description(desc = "nmaac.autoexeccombopversiongetapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(id);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(id);
        }
        Integer isActive = autoexecCombopMapper.getAutoexecCombopIsActiveByIdForUpdate(autoexecCombopVersionVo.getCombopId());
        if (Objects.equals(autoexecCombopVersionVo.getIsActive(), 1) && Objects.equals(isActive, 1)) {
            try {
                autoexecCombopService.verifyAutoexecCombopVersionConfig(autoexecCombopVersionVo.getConfig(), false);
            } catch (Exception e) {
                AutoexecCombopVo autoexecCombopVo = new AutoexecCombopVo();
                autoexecCombopVo.setId(autoexecCombopVersionVo.getCombopId());
                autoexecCombopVo.setLcu(UserContext.get().getUserUuid());
                autoexecCombopMapper.updateAutoexecCombopIsActiveById(autoexecCombopVo);

                JSONObject configExpiredReason = new JSONObject();
                JSONArray reasonList = new JSONArray();
                JSONObject errorMessageObj = new JSONObject();
                errorMessageObj.put("description", e.getMessage());
                reasonList.add(errorMessageObj);
                configExpiredReason.put("reasonList", reasonList);
                autoexecCombopVersionVo.setConfigExpired(1);
                autoexecCombopVersionVo.setConfigExpiredReason(configExpiredReason);
                autoexecCombopVersionMapper.updateAutoexecCombopVersionConfigExpiredById(autoexecCombopVersionVo);
            }
        }
        return autoexecCombopVersionVo;
    }
}
