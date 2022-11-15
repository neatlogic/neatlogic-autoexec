package codedriver.module.autoexec.notify.handler.param;

import codedriver.framework.autoexec.constvalue.AutoexecJobNotifyParam;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.notify.AutoexecJobNotifyParamHandlerBase;
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
