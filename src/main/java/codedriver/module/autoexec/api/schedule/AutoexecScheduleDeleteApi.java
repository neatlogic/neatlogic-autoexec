package codedriver.module.autoexec.api.schedule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.autoexec.exception.AutoexecScheduleNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.autoexec.schedule.plugin.AutoexecScheduleJob;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
//@AuthAction(action = SCHEDULE_JOB_MODIFY.class)

@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecScheduleDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;

    @Override
    public String getToken() {
        return "autoexec/schedule/delete";
    }

    @Override
    public String getName() {
        return "删除定时作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "定时作业uuid")
    })
    @Description(desc = "删除定时作业")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String uuid = paramObj.getString("uuid");
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        if (autoexecScheduleVo == null) {
            throw new AutoexecScheduleNotFoundException(uuid);
        }
        String tenantUuid = TenantContext.get().getTenantUuid();
        IJob jobHandler = SchedulerManager.getHandler(AutoexecScheduleJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(AutoexecScheduleJob.class.getName());
        }
        JobObject jobObject = new JobObject.Builder(uuid, jobHandler.getGroupName(), jobHandler.getClassName(), tenantUuid).build();
//        schedulerManager.unloadJob(jobObject);
//        schedulerMapper.deleteJobAuditByJobUuid(jobUuid);
        autoexecScheduleMapper.deleteAutoexecScheduleByUuid(uuid);
        return null;
    }

}
