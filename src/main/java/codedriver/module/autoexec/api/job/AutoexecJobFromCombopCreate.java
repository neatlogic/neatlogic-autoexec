/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecJobFromCombopCreate extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "操作组ID")
    })
    @Output({
    })
    @Description(desc = "作业创建（来自操作组）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        return null;
    }

    @Override
    public String getToken() {
        return null;
    }
}
