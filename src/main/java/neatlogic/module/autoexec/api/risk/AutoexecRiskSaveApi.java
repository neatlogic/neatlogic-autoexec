/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.risk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import neatlogic.framework.autoexec.exception.AutoexecRiskNameRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecRiskNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecRiskService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecRiskSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;
    @Resource
    private AutoexecRiskService autoexecRiskService;

    @Override
    public String getToken() {
        return "autoexec/risk/save";
    }

    @Override
    public String getName() {
        return "nmaar.autoexecrisksaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 50, isRequired = true, desc = "common.name"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "common.isactive"),
            @Param(name = "color", type = ApiParamType.STRING, isRequired = true, desc = "common.color"),
            @Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "common.description"),
    })
    @Output({
    })
    @Description(desc = "nmaar.autoexecrisksaveapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecRiskVo vo = jsonObj.toJavaObject(AutoexecRiskVo.class);
        Long id = jsonObj.getLong("id");
        if (autoexecRiskMapper.checkRiskNameIsRepeats(vo) > 0) {
            throw new AutoexecRiskNameRepeatException(vo.getName());
        }
        if (id != null) {
            AutoexecRiskVo risk = autoexecRiskMapper.getAutoexecRiskById(id);
            if (risk == null) {
                throw new AutoexecRiskNotFoundException(id);
            }
        }
        autoexecRiskService.saveRisk(vo);
//        if (id != null) {
//            AutoexecRiskVo risk = autoexecRiskMapper.getAutoexecRiskById(id);
//            if (risk == null) {
//                throw new AutoexecRiskNotFoundException(id);
//            }
//            vo.setSort(risk.getSort());
//            autoexecRiskMapper.updateRisk(vo);
//        } else {
//            Integer sort = autoexecRiskMapper.getMaxSort();
//            vo.setSort(sort != null ? sort + 1 : 1);
//            autoexecRiskMapper.insertRisk(vo);
//        }
        return null;
    }

    public IValid name() {
        return value -> {
            AutoexecRiskVo vo = JSON.toJavaObject(value, AutoexecRiskVo.class);
            if (autoexecRiskMapper.checkRiskNameIsRepeats(vo) > 0) {
                return new FieldValidResultVo(new AutoexecRiskNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
