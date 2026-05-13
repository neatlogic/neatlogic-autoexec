/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.schedule;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCHEDULE_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScheduleNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dao.mapper.SchedulerMapper;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.dto.JobStatusVo;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.autoexec.schedule.plugin.AutoexecScheduleJob;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/10/14 14:17
 **/
@Service
@AuthAction(action = AUTOEXEC_SCHEDULE_MODIFY.class)
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecScheduleIsActiveUpdateApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Resource
    private SchedulerManager schedulerManager;
    @Resource
    private SchedulerMapper schedulerMapper;

    @Override
    public String getToken() {
        return "autoexec/schedule/isactive/update";
    }

    @Override
    public String getName() {
        return "启用/禁用定时作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "定时作业id")
    })
    @Output({})
    @Description(desc = "启用/禁用定时作业")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleById(id);
        if (autoexecScheduleVo == null) {
            throw new AutoexecScheduleNotFoundException(id);
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecScheduleVo.getAutoexecCombopId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecScheduleVo.getAutoexecCombopId());
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getExecutable(), 0)) {
            throw new PermissionDeniedException();
        }
        autoexecScheduleVo.setLcu(UserContext.get().getUserUuid(true));
        autoexecScheduleMapper.updateAutoexecScheduleIsActiveById(autoexecScheduleVo);
        autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleById(id);
        IJob jobHandler = SchedulerManager.getHandler(AutoexecScheduleJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(AutoexecScheduleJob.class.getName());
        }
        String tenantUuid = TenantContext.get().getTenantUuid();

        JobObject jobObject = new JobObject.Builder(autoexecScheduleVo.getUuid(), jobHandler.getGroupName(), jobHandler.getClassName(), tenantUuid)
                .withCron(autoexecScheduleVo.getCron()).withBeginTime(autoexecScheduleVo.getBeginTime())
                .withEndTime(autoexecScheduleVo.getEndTime())
//                .needAudit(autoexecScheduleVo.getNeedAudit())
                .setType("private")
                .build();
        if (Objects.equals(autoexecScheduleVo.getIsActive(), 1)) {
            schedulerManager.loadJob(jobObject);
        } else {
            schedulerManager.unloadJob(jobObject);
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("isActive", autoexecScheduleVo.getIsActive());
        resultObj.put("lcd", autoexecScheduleVo.getLcd());
        resultObj.put("lcu", autoexecScheduleVo.getLcu());
        resultObj.put("lcuVo", autoexecScheduleVo.getLcuVo());
        JobStatusVo jobStatusVo = schedulerMapper.getJobStatusByJobNameGroup(autoexecScheduleVo.getUuid(), jobHandler.getGroupName(), System.currentTimeMillis());
        resultObj.put("jobStatus", jobStatusVo);
        return resultObj;
    }
}
