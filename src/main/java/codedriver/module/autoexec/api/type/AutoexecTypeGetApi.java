/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.type;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecTypeGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/get";
    }

    @Override
    public String getName() {
        return "获取工具类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "类型ID"),
    })
    @Output({
            @Param(explode = AutoexecTypeVo.class)
    })
    @Description(desc = "获取工具类型")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecTypeVo type = autoexecTypeMapper.getTypeById(id);
        if (type == null) {
            throw new AutoexecTypeNotFoundException(id);
        }
        return type;
    }


}
