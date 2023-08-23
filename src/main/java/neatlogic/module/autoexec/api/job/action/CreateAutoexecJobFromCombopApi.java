/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
        return "nmaaja.createautoexecjobfromcombopapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.combopid"),
            @Param(name = "combopVersionId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.versionid"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.name"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "term.autoexec.executeparam"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.source"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.invokeid"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.parentid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.scenarioname"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.roundcount "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.triggertype"),
            @Param(name = "assignExecUser", type = ApiParamType.STRING, desc = "term.autoexec.assignexecuser")
    })
    @Output({
    })
    @Description(desc = "nmaaja.createautoexecjobfromcombopapi.getname")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        jsonObj.put("operationType", CombopOperationType.COMBOP.getValue());
        jsonObj.put("operationId", jsonObj.getLong("combopId"));
        AutoexecJobVo autoexecJobParam = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);

        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobParam);
        String triggerType = jsonObj.getString("triggerType");
        Long planStartTime = jsonObj.getLong("planStartTime");
        autoexecJobActionService.settingJobFireMode(triggerType, planStartTime, autoexecJobParam);

//        //如果是自动开始且计划开始时间小于等于当前时间则直接激活作业
//        if (Objects.equals(JobTriggerType.AUTO.getValue(), jsonObj.getString("triggerType")) && (jsonObj.containsKey("planStartTime") && jsonObj.getLong("planStartTime") <= System.currentTimeMillis())) {
//            fireJob(autoexecJobParam);
//            return new JSONObject() {{
//                put("jobId", autoexecJobParam.getId());
//            }};
//        }
//
//
//        if (jsonObj.containsKey("triggerType")) {
//            // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
//            if (JobTriggerType.AUTO.getValue().equals(jsonObj.getString("triggerType"))) {
//                if (!jsonObj.containsKey("planStartTime")) {
//                    throw new ParamIrregularException("planStartTime");
//                }
//                IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
//                if (jobHandler == null) {
//                    throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
//                }
//                JobObject.Builder jobObjectBuilder = new JobObject.Builder(autoexecJobParam.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
//                jobHandler.reloadJob(jobObjectBuilder.build());
//            }
//        } else {
//            fireJob(autoexecJobParam);
//        }
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
