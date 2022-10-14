/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecJobFromCombopApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "作业创建（来自组合工具）";
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
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "分组数 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "auto,manual", desc = "触发方式"),
            @Param(name = "assignExecUser", type = ApiParamType.STRING, desc = "指定执行用户")
    })
    @Output({
    })
    @Description(desc = "作业创建（来自组合工具）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        jsonObj.put("operationType", CombopOperationType.COMBOP.getValue());
        jsonObj.put("operationId", jsonObj.getLong("combopId"));
        if (!jsonObj.containsKey("invokeId")) {
            jsonObj.put("invokeId", jsonObj.getLong("combopId"));
        }
        AutoexecJobVo autoexecJobParam = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);

        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobParam);

        //如果是自动开始且计划开始时间小于等于当前时间则直接激活作业
        if (Objects.equals(JobTriggerType.AUTO.getValue(), jsonObj.getString("triggerType")) && (jsonObj.containsKey("planStartTime") && jsonObj.getLong("planStartTime") <= System.currentTimeMillis())) {
            fireJob(autoexecJobParam);
            return new JSONObject() {{
                put("jobId", autoexecJobParam.getId());
            }};
        }


        if (jsonObj.containsKey("triggerType")) {
            // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
            if (JobTriggerType.AUTO.getValue().equals(jsonObj.getString("triggerType"))) {
                if (!jsonObj.containsKey("planStartTime")) {
                    throw new ParamIrregularException("planStartTime");
                }
                IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
                if (jobHandler == null) {
                    throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
                }
                JobObject.Builder jobObjectBuilder = new JobObject.Builder(autoexecJobParam.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
                jobHandler.reloadJob(jobObjectBuilder.build());
            }
        } else {
            fireJob(autoexecJobParam);
        }
        return new JSONObject() {{
            put("jobId", autoexecJobParam.getId());
        }};
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create";
    }


    private void fireJob(AutoexecJobVo autoexecJobParam) throws Exception {
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        autoexecJobParam.setAction(JobAction.FIRE.getValue());
        autoexecJobParam.setIsFirstFire(1);
        fireAction.doService(autoexecJobParam);
    }
}
