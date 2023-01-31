/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.risk;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecRiskListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public String getToken() {
        return "autoexec/risk/list";
    }

    @Override
    public String getName() {
        return "获取操作级别列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, explode = ValueTextVo[].class, desc = "操作级别列表"),
    })
    @Description(desc = "获取操作级别列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ValueTextVo> riskList = autoexecRiskMapper.getAllActiveRisk();
        return riskList;
    }


}
