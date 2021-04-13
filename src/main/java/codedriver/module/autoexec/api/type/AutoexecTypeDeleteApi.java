/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.type;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.exception.AutoexecTypeHasBeenReferredException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecTypeDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/delete";
    }

    @Override
    public String getName() {
        return "删除插件类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "类型ID")})
    @Output({})
    @Description(desc = "删除插件类型")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (autoexecTypeMapper.checkTypeIsExistsById(id) == 0) {
            throw new AutoexecTypeNotFoundException(id);
        }
        AutoexecTypeVo type = autoexecTypeMapper.getTypeById(id);
        // 已经被工具或脚本引用的分类不可删除
        if (autoexecTypeMapper.checkTypeHasBeenReferredById(id) > 0) {
            throw new AutoexecTypeHasBeenReferredException(type.getName());
        }
        autoexecTypeMapper.deleteTypeById(id);
        return null;
    }


}
