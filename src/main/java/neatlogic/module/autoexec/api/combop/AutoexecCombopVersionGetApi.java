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
        if (Objects.equals(autoexecCombopVersionVo.getIsActive(), 1)) {
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
            }
        }
        return autoexecCombopVersionVo;
    }
}
