package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopVersionStatusUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/version/status/update";
    }

    @Override
    public String getName() {
        return "更新组合工具版本状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "组合工具版本id"),
            @Param(name = "status", type =ApiParamType.ENUM, rule = "draft,passed,rejected", isRequired = true, desc = "状态")
    })
    @Description(desc = "更新组合工具版本状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(id);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(id);
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        String status = paramObj.getString("status");
        autoexecCombopVersionVo.setStatus(status);
        if (Objects.equals(status, ScriptVersionStatus.PASSED.getValue()) || Objects.equals(status, ScriptVersionStatus.REJECTED.getValue())) {
            if (Objects.equals(autoexecCombopVo.getViewable(), 0)) {
                throw new PermissionDeniedException();
            }
        } else if (Objects.equals(status, ScriptVersionStatus.DRAFT.getValue())) {
            if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
                throw new PermissionDeniedException();
            }
        }
        if (Objects.equals(status, ScriptVersionStatus.PASSED.getValue())) {
            autoexecCombopVersionVo.setReviewer(UserContext.get().getUserUuid());
            autoexecCombopVersionMapper.disableAutoexecCombopVersionByCombopId(autoexecCombopVersionVo.getCombopId());
            autoexecCombopVersionMapper.enableAutoexecCombopVersionById(id);
        } else {
            autoexecCombopVersionVo.setReviewer(null);
        }
        autoexecCombopVersionMapper.updateAutoexecCombopVersionStatusById(autoexecCombopVersionVo);
        return null;
    }
}
