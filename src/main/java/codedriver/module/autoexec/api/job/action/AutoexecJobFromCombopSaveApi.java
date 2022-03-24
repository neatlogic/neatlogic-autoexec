/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author laiwt
 * @since 2022/3/23 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecJobFromCombopSaveApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "作业保存（来自组合工具）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源 itsm|human   ITSM|人工发起的等，不传默认是人工发起的"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "来源id"),
            @Param(name = "threadCount", type = ApiParamType.LONG, isRequired = true, desc = "并发线程,2的n次方 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, isRequired = true, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "auto,manual", isRequired = true, desc = "触发方式"),
    })
    @Output({
    })
    @Description(desc = "作业保存（来自组合工具）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecJobVo jobVo = autoexecJobActionService.validateCreateJobFromCombop(jsonObj, true);
        // todo 保存之后，如果设置的人工触发，只有点执行按钮才能触发，如果是自动触发，则启动一个定时作业，如果没到点就人工触发了，则取消定时作业，立即执行
        if (JobTriggerType.AUTO.getValue().equals(jsonObj.getString("triggerType"))) {
            // todo 启动定时作业
        }
        return new JSONObject() {{
            put("jobId", jobVo.getId());
        }};

    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/save";
    }
}
