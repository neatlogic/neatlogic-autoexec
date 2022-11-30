/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.type;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.exception.AutoexecTypeNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecTypeSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/save";
    }

    @Override
    public String getName() {
        return "保存自动化工具分类";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "类型ID"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, desc = "名称"),
            @Param(name = "description", type = ApiParamType.STRING, maxLength = 500, desc = "描述"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "授权列表")
    })
    @Output({})
    @Description(desc = "保存自动化工具分类")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecTypeVo typeVo = JSON.toJavaObject(jsonObj, AutoexecTypeVo.class);
        if (autoexecTypeMapper.checkTypeNameIsExists(typeVo) > 0) {
            throw new AutoexecTypeNameRepeatException(typeVo.getName());
        }
        typeVo.setLcu(UserContext.get().getUserUuid());
        if (jsonObj.getLong("id") == null) {
            autoexecTypeMapper.insertType(typeVo);
        } else {
            autoexecTypeMapper.deleteTypeAuthByTypeId(typeVo.getId());
            if (autoexecTypeMapper.checkTypeIsExistsById(typeVo.getId()) == 0) {
                throw new AutoexecTypeNotFoundException(typeVo.getId());
            }
            autoexecTypeMapper.updateType(typeVo);
        }
        autoexecTypeMapper.insertTypeAuth(typeVo.getAutoexecTypeAuthList());
        return null;
    }

    public IValid name() {
        return value -> {
            AutoexecTypeVo typeVo = JSON.toJavaObject(value, AutoexecTypeVo.class);
            if (autoexecTypeMapper.checkTypeNameIsExists(typeVo) > 0) {
                return new FieldValidResultVo(new AutoexecTypeNameRepeatException(typeVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }


}
