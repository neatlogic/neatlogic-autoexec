/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
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
        for (int currentPage = 1; rowNum > 0; currentPage++, rowNum -= 100) {
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
            jobVo.setIsFirstFire(1);
            UserVo fcuVo = userMapper.getUserByUuid(autoexecScheduleVo.getFcu());
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(autoexecScheduleVo.getFcu());
            UserContext.init(fcuVo, authenticationInfoVo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(fcuVo).getCc());
            autoexecJobActionService.validateAndCreateJobFromCombop(jobVo);
            jobVo.setAction(JobAction.FIRE.getValue());
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }
}
