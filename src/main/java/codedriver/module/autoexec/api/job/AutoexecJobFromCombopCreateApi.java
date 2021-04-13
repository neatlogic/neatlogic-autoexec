/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Transactional
@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecJobFromCombopCreateApi extends PrivateApiComponentBase {
    @Resource
    AutoexecService autoexecService;

    @Override
    public String getName() {
        return "作业创建（来自操作组）";
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

        autoexecService.saveAutoexecJob();
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create";
    }
}
