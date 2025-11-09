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

package neatlogic.module.autoexec.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.dto.JobVo;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/9/29 17:42
 **/
@Component
@DisallowConcurrentExecution
public class AutoexecScheduleJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(AutoexecScheduleJob.class);
    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Resource
    UserMapper userMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-AUTOEXEC-SCHEDULE-JOB";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        String uuid = jobObject.getJobName();
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        if (autoexecScheduleVo == null) {
            return false;
        }
        if (jobObject.isTest() == 1) {
            return true;
        }
        return Objects.equals(autoexecScheduleVo.getIsActive(), 1) && Objects.equals(autoexecScheduleVo.getCron(), jobObject.getCron());
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        String uuid = jobObject.getJobName();
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        if (autoexecScheduleVo != null) {
            JobObject newJobObjectBuilder = new JobObject.Builder(autoexecScheduleVo.getUuid(), this.getGroupName(), this.getClassName(), tenantUuid)
                    .withCron(autoexecScheduleVo.getCron()).withBeginTime(autoexecScheduleVo.getBeginTime())
                    .withEndTime(autoexecScheduleVo.getEndTime())
//                .needAudit(autoexecScheduleVo.getNeedAudit())
                    .build();
            schedulerManager.loadJob(newJobObjectBuilder);
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        AutoexecScheduleVo searchVo = new AutoexecScheduleVo();
        searchVo.setIsActive(1);
        int rowNum = autoexecScheduleMapper.getAutoexecScheduleCount(searchVo);
        searchVo.setPageSize(100);
        searchVo.setRowNum(rowNum);
        Integer pageCount = searchVo.getPageCount();
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<AutoexecScheduleVo> autoexecScheduleList = autoexecScheduleMapper.getAutoexecScheduleList(searchVo);
            for (AutoexecScheduleVo autoexecScheduleVo : autoexecScheduleList) {
                JobObject.Builder jobObjectBuilder = new JobObject
                        .Builder(autoexecScheduleVo.getUuid(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
                JobObject jobObject = jobObjectBuilder.build();
                this.reloadJob(jobObject);
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        String uuid = jobObject.getJobName();
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        if (autoexecScheduleVo == null) {
            schedulerManager.unloadJob(jobObject);
            return;
        }
        Long combopId = autoexecScheduleVo.getAutoexecCombopId();
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            schedulerManager.unloadJob(jobObject);
        } else {
//        System.out.println(new Date() + "执行定时作业：'" + autoexecScheduleVo.getName() + "'");
            AutoexecJobVo jobVo;
            if (MapUtils.isNotEmpty(autoexecScheduleVo.getConfig())) {
                jobVo = autoexecScheduleVo.getConfig().toJavaObject(AutoexecJobVo.class);
            } else {
                jobVo = new AutoexecJobVo();
            }
            jobVo.setOperationId(combopId);
            jobVo.setSource(JobSource.AUTOEXEC_SCHEDULE.getValue());
            jobVo.setInvokeId(autoexecScheduleVo.getId());
            jobVo.setRouteId(autoexecScheduleVo.getId().toString());
            jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
            String execUserUuid = autoexecScheduleVo.getLcu();
            if (jobObject.isTest() == 1 && StringUtils.isNotBlank(jobObject.getTestUserUuid())) {
                execUserUuid = jobObject.getTestUserUuid();
            }
            UserVo execUser = userMapper.getUserBaseInfoByUuid(execUserUuid);
            if (execUser == null) {
                schedulerManager.unloadJob(jobObject);
                logger.error("execUser: {} not exist!", execUserUuid);
                return;
            }
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(autoexecScheduleVo.getFcu());
            UserContext.init(execUser, authenticationInfoVo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
            autoexecJobActionService.validateAndCreateJobFromCombop(jobVo);
            jobVo.setAction(JobAction.FIRE.getValue());
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }

    @Override
    public JobVo getJob(String uuid) throws Exception {
        JobVo jobVo = null;
        AutoexecScheduleVo scheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        if (scheduleVo != null) {
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(scheduleVo.getAutoexecCombopId());
            if (autoexecCombopVo == null) {
                throw new AutoexecCombopNotFoundException(scheduleVo.getAutoexecCombopId());
            }
            autoexecCombopService.setOperableButtonList(autoexecCombopVo);
            if (Objects.equals(autoexecCombopVo.getExecutable(), 0)) {
                throw new PermissionDeniedException();
            }
            jobVo = new JobVo();
            jobVo.setName(scheduleVo.getName());
            jobVo.setUuid(scheduleVo.getUuid());
            jobVo.setCron(scheduleVo.getCron());
            jobVo.setIsActive(scheduleVo.getIsActive());
        }
        return jobVo;
    }
}
