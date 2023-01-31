package neatlogic.module.autoexec.notify.handler.param;

import neatlogic.framework.autoexec.constvalue.AutoexecJobNotifyParam;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.notify.AutoexecJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

@Component
public class AutoexecJobNameParamHandler extends AutoexecJobNotifyParamHandlerBase {
    @Override
    public Object getMyText(AutoexecJobVo autoexecJobVo) {
        if (autoexecJobVo != null) {
            return autoexecJobVo.getName();
        }
        return null;
    }

    @Override
    public String getValue() {
        return AutoexecJobNotifyParam.NAME.getValue();
    }
}
