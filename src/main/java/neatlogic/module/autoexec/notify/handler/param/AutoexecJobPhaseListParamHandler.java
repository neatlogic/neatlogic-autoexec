package neatlogic.module.autoexec.notify.handler.param;

import neatlogic.framework.autoexec.constvalue.AutoexecJobNotifyParam;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.notify.AutoexecJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AutoexecJobPhaseListParamHandler extends AutoexecJobNotifyParamHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public Object getMyText(AutoexecJobVo autoexecJobVo) {
        if (autoexecJobVo != null) {
            return autoexecJobMapper.getJobPhaseListByJobId(autoexecJobVo.getId());
        }
        return null;
    }

    @Override
    public String getValue() {
        return AutoexecJobNotifyParam.PHASELIST.getValue();
    }
}
