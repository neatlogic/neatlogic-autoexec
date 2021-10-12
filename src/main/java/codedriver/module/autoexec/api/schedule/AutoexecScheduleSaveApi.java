package codedriver.module.autoexec.api.schedule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScheduleNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecScheduleNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.framework.scheduler.exception.ScheduleIllegalParameterException;
import codedriver.framework.scheduler.exception.ScheduleJobNameRepeatException;
import codedriver.module.autoexec.schedule.plugin.AutoexecScheduleJob;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScheduleSaveApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getToken() {
        return "autoexec/schedule/save";
    }

    @Override
    public String getName() {
        return "保存定时作业信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "定时作业id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "定时作业名称"),
            @Param(name = "autoexecCombopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具id"),
            @Param(name = "beginTime", type = ApiParamType.LONG, desc = "开始时间"),
            @Param(name = "endTime", type = ApiParamType.LONG, desc = "结束时间"),
            @Param(name = "cron", type = ApiParamType.STRING, isRequired = true, desc = "corn表达式"),
            @Param(name = "isActive", type = ApiParamType.ENUM, isRequired = true, rule = "0,1", desc = "是否激活(0:禁用，1：激活)"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "执行配置信息，包含param、executeConfig、threadCount等字段")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.STRING, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "保存定时作业信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long autoexecCombopId = paramObj.getLong("autoexecCombopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecCombopId);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getExecutable(), 0)) {
            throw new PermissionDeniedException();
        }
        String cron = paramObj.getString("cron");
        if (!CronExpression.isValidExpression(cron)) {
            throw new ScheduleIllegalParameterException(cron);
        }
        AutoexecScheduleVo autoexecScheduleVo = paramObj.toJavaObject(AutoexecScheduleVo.class);
        if (autoexecScheduleMapper.checkAutoexecScheduleNameIsExists(autoexecScheduleVo) > 0) {
            throw new ScheduleJobNameRepeatException(autoexecScheduleVo.getName());
        }
        Long id = paramObj.getLong("id");
        if (id != null) {
            AutoexecScheduleVo oldAutoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleById(id);
            if (oldAutoexecScheduleVo == null) {
                throw new AutoexecScheduleNotFoundException(id);
            }
            autoexecScheduleVo.setLcu(UserContext.get().getUserUuid(true));
            autoexecScheduleMapper.updateAutoexecSchedule(autoexecScheduleVo);
        } else {
            autoexecScheduleVo.setFcu(UserContext.get().getUserUuid(true));
            autoexecScheduleMapper.insertAutoexecSchedule(autoexecScheduleVo);
        }

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
        if (autoexecScheduleVo.getIsActive().intValue() == 1) {
            schedulerManager.loadJob(jobObject);
        } else {
            schedulerManager.unloadJob(jobObject);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("id", autoexecScheduleVo.getId());
        return resultObj;
    }

    public IValid name() {
        return value -> {
            AutoexecScheduleVo vo = JSONObject.toJavaObject(value, AutoexecScheduleVo.class);
            if (autoexecScheduleMapper.checkAutoexecScheduleNameIsExists(vo) > 0) {
                return new FieldValidResultVo(new AutoexecScheduleNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
