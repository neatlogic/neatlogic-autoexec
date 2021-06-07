/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 更新组合工具状态接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopIsActiveUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/isactive/update";
    }

    @Override
    public String getName() {
        return "更新组合工具状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.INTEGER, desc = "更新后的状态")
    })
    @Description(desc = "更新组合工具状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer isActive = autoexecCombopMapper.getAutoexecCombopIsActiveByIdForUpdate(id);
        if (isActive == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
            throw new PermissionDeniedException();
        }
        /** 如果是激活组合工具，则需要校验该组合工具配置正确 **/
        if (isActive == 0) {
            autoexecCombopService.verifyAutoexecCombopConfig(autoexecCombopVo);
        }
        autoexecCombopVo.setLcu(UserContext.get().getUserUuid(true));
        autoexecCombopMapper.updateAutoexecCombopIsActiveById(autoexecCombopVo);
        return (1 - isActive);
    }
}
