/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * @author linbq
 * @since 2021/9/6 19:35
 **/
@Component
public class AutoexecJobStatusMonitorJob extends JobBase {
    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-AUTOEXEC-JOB-STATUS-MONITOR";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
        return null;
    }

    @Override
    public void reloadJob(JobObject jobObject) {

    }

    @Override
    public void initJob(String tenantUuid) {

    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {

    }
}
