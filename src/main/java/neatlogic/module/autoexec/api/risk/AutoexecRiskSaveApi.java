/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.risk;

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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecRiskSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public String getToken() {
        return "autoexec/risk/save";
    }

    @Override
    public String getName() {
        return "保存操作级别";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 50, isRequired = true, desc = "名称"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "状态"),
            @Param(name = "color", type = ApiParamType.STRING, isRequired = true, desc = "颜色"),
            @Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "描述"),
    })
    @Output({
    })
    @Description(desc = "保存操作级别")
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
            vo.setSort(risk.getSort());
            autoexecRiskMapper.updateRisk(vo);
        } else {
            Integer sort = autoexecRiskMapper.getMaxSort();
            vo.setSort(sort != null ? sort + 1 : 1);
            autoexecRiskMapper.insertRisk(vo);
        }
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
