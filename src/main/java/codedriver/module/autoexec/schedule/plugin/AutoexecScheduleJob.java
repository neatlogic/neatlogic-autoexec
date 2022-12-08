/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobSource;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
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
        }else {
//        System.out.println(new Date() + "执行定时作业：'" + autoexecScheduleVo.getName() + "'");
            JSONObject paramObj = autoexecScheduleVo.getConfig();
            paramObj.put("combopId", combopId);
            paramObj.put("source", JobSource.AUTOEXEC_SCHEDULE.getValue());
            paramObj.put("invokeId", autoexecScheduleVo.getId());
            paramObj.put("operationId", autoexecCombopVo.getId());
            paramObj.put("operationType", CombopOperationType.COMBOP.getValue());
            paramObj.put("isFirstFire", 1);
            UserVo fcuVo = userMapper.getUserByUuid(autoexecScheduleVo.getFcu());
            UserContext.init(fcuVo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(fcuVo).getCc());
            AutoexecJobVo jobVo = JSONObject.toJavaObject(paramObj, AutoexecJobVo.class);
            autoexecJobActionService.validateAndCreateJobFromCombop(jobVo);
            jobVo.setAction(JobAction.FIRE.getValue());
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }
}
