package codedriver.module.autoexec.notify.handler.param;

import codedriver.framework.autoexec.constvalue.AutoexecJobNotifyParam;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.notify.AutoexecJobNotifyParamHandlerBase;
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
