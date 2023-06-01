package neatlogic.module.autoexec.message.handler;

import neatlogic.framework.message.core.MessageHandlerBase;
import neatlogic.framework.notify.dto.NotifyVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutoexecJobMessageHandler extends MessageHandlerBase {

    @Override
    public String getName() {
        return "自动化作业";
    }

    @Override
    public String getDescription() {
        return "实时显示自动化作业相关信息";
    }

    @Override
    public boolean getNeedCompression() {
        return false;
    }

    @Override
    public NotifyVo compress(List<NotifyVo> notifyVoList) {
        return null;
    }
}
