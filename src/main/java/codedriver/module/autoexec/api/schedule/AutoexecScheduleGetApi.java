package codedriver.module.autoexec.api.schedule;

import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.autoexec.exception.AutoexecScheduleNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
//@AuthAction(action = SCHEDULE_JOB_MODIFY.class)

@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScheduleGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;

    @Override
    public String getToken() {
        return "autoexec/schedule/get";
    }

    @Override
    public String getName() {
        return "获取定时作业信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "定时作业uuid")
    })
    @Description(desc = "获取定时作业信息")
    @Output({
            @Param(name = "Return", explode = AutoexecScheduleVo.class, desc = "定时作业信息")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String uuid = paramObj.getString("uuid");
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        if (autoexecScheduleVo == null) {
            throw new AutoexecScheduleNotFoundException(uuid);
        }
        return autoexecScheduleVo;
    }

}
