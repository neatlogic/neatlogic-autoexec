/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 删除组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/delete";
    }

    @Override
    public String getName() {
        return "删除组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Description(desc = "删除组合工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo != null) {
            autoexecCombopService.setOperableButtonList(autoexecCombopVo);
            if (Objects.equals(autoexecCombopVo.getDeletable(), 0)) {
                throw new PermissionDeniedException();
            }
            List<Long> combopPhaseIdList = autoexecCombopMapper.getCombopPhaseIdListByCombopId(id);
            autoexecCombopMapper.deleteAutoexecCombopById(id);
            autoexecCombopMapper.deleteAutoexecCombopAuthorityByCombopId(id);
            autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(id);
            if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
                autoexecCombopMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
            }
            autoexecCombopMapper.deleteAutoexecCombopPhaseByCombopId(id);
        }
        return null;
    }
}
