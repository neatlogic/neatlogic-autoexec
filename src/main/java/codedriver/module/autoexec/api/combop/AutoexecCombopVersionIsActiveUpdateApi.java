package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_REVIEW;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_REVIEW.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecCombopVersionIsActiveUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/version/isactive/update";
    }

    @Override
    public String getName() {
        return "激活组合工具版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "组合工具版本id")
    })
    @Description(desc = "激活组合工具版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(id);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(id);
        }
        if (autoexecCombopVersionVo.getIsActive() == 1) {
            return null;
        }
        if (!Objects.equals(autoexecCombopVersionVo.getStatus(), "passed")) {
            // TODO 抛异常
            return null;
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
//        if (Objects.equals(autoexecCombopVo.getDeletable(), 0)) {
//            throw new PermissionDeniedException();
//        }
        autoexecCombopVersionMapper.disableAutoexecCombopVersionByCombopId(autoexecCombopVersionVo.getCombopId());
        autoexecCombopVersionMapper.enableAutoexecCombopVersionById(id);
        return null;
    }
}
