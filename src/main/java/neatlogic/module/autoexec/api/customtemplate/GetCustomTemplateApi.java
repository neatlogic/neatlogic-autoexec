/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.customtemplate;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCustomTemplateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;


    @Input({
            @Param(name = "id", isRequired = true, type = ApiParamType.LONG, desc = "id")
    })
    @Output({
            @Param(explode = CustomTemplateVo.class)
    })
    @Description(desc = "获取自定义模板接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return autoexecCustomTemplateMapper.getCustomTemplateById(jsonObj.getLong("id"));
    }


    @Override
    public String getName() {
        return "获取自定义模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/customtemplate/get";
    }
}
