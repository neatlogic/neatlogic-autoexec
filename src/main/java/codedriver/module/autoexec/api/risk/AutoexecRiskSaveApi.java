/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.risk;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_RISK_MODIFY;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.autoexec.exception.AutoexecRiskNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecRiskNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_RISK_MODIFY.class)
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
