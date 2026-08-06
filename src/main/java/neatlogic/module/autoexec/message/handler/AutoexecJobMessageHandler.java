package neatlogic.module.autoexec.message.handler;

import neatlogic.framework.util.$;

import neatlogic.framework.message.core.MessageHandlerBase;
import neatlogic.framework.notify.dto.NotifyVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutoexecJobMessageHandler extends MessageHandlerBase {

    @Override
    public String getName() {
        return "nmaa.autoexecjobmessagehandler.getname";
    }

    @Override
    public String getDescription() {
        return $.t("nmaa.autoexecjobmessagehandler.getdescription");
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
