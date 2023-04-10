/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.schedule.plugin.AutoexecJobAutoFireJob;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecCombopJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "组合工具创建作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "combopVersionId", type = ApiParamType.LONG, desc = "组合工具版本ID"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "分组数 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "触发方式")
    })
    @Output({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID")
    })
    @Description(desc = "组合工具创建作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = paramObj.toJavaObject(AutoexecJobVo.class);
        Long combopId = paramObj.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        jobVo.setOperationId(combopId);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        Long combopVersionId = paramObj.getLong("combopVersionId");
        if (combopVersionId != null) {
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(combopVersionId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(combopVersionId);
            }
            jobVo.setCombopVersionId(combopVersionId);
            jobVo.setInvokeId(combopVersionId);
            jobVo.setRouteId(combopVersionId.toString());
            jobVo.setSource(JobSource.COMBOP_TEST.getValue());
        } else {
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(combopId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
            }
            jobVo.setCombopVersionId(autoexecCombopVersionVo.getId());
            jobVo.setInvokeId(autoexecCombopVersionVo.getId());;
            jobVo.setRouteId(autoexecCombopVersionVo.getId().toString());
            jobVo.setSource(JobSource.COMBOP.getValue());
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(jobVo);
        //如果是自动开始且计划开始时间小于等于当前时间则直接激活作业
        if (Objects.equals(JobTriggerType.AUTO.getValue(), paramObj.getString("triggerType")) && (paramObj.containsKey("planStartTime") && paramObj.getLong("planStartTime") <= System.currentTimeMillis())) {
            fireJob(jobVo);
            return new JSONObject() {{
                put("jobId", jobVo.getId());
            }};
        }


        if (paramObj.containsKey("triggerType")) {
            // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
            if (JobTriggerType.AUTO.getValue().equals(paramObj.getString("triggerType"))) {
                if (!paramObj.containsKey("planStartTime")) {
                    throw new ParamIrregularException("planStartTime");
                }
                IJob jobHandler = SchedulerManager.getHandler(AutoexecJobAutoFireJob.class.getName());
                if (jobHandler == null) {
                    throw new ScheduleHandlerNotFoundException(AutoexecJobAutoFireJob.class.getName());
                }
                JobObject.Builder jobObjectBuilder = new JobObject.Builder(jobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
                jobHandler.reloadJob(jobObjectBuilder.build());
            }
        } else {
            fireJob(jobVo);
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", jobVo.getId());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "/autoexec/combop/job/create";
    }

    private void fireJob(AutoexecJobVo autoexecJobParam) throws Exception {
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        autoexecJobParam.setAction(JobAction.FIRE.getValue());
        autoexecJobParam.setIsFirstFire(1);
        fireAction.doService(autoexecJobParam);
    }
}
