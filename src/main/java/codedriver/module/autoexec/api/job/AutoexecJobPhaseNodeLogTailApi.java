/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/5/13 16:49
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeLogTailApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobActionService autoexecJobActionService;
    @Override
    public String getName() {
        return "实时获取剧本节点执行日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nodeId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本节点Id"),
            @Param(name = "position", type = ApiParamType.LONG, isRequired = true, desc = "日志读取位置")
    })
    @Output({
            @Param(name = "startPos", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本节点Id"),
            @Param(name = "endPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取位置"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取位置"),
            @Param(name = "last", type = ApiParamType.LONG, isRequired = true, desc = "日志读取位置"),
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return autoexecJobActionService.tailNodeLog(paramObj);
    }

    @Override
    public String getToken() {
        return "/autoexec/job/phase/node/log/tail";
    }
}
