package neatlogic.module.autoexec.api.schedule;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import neatlogic.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import neatlogic.framework.autoexec.exception.AutoexecScheduleNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScheduleGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

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
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "获取定时作业信息")
    @Output({
            @Param(name = "Return", explode = AutoexecScheduleVo.class, desc = "定时作业信息")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleById(id);
        if (autoexecScheduleVo == null) {
            throw new AutoexecScheduleNotFoundException(id);
        }
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(autoexecScheduleVo.getAutoexecCombopId()) == 0) {
            autoexecScheduleVo.setAutoexecCombopId(null);
        }
        return autoexecScheduleVo;
    }

}
