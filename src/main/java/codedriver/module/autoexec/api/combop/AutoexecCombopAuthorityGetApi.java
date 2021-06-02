/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_EXECUTE;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopAuthorityVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询组合工具授权信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@AuthAction(action = AUTOEXEC_COMBOP_EXECUTE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopAuthorityGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/authority/get";
    }

    @Override
    public String getName() {
        return "查询组合工具授权信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, desc = "编辑授权列表"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, desc = "执行授权列表")
    })
    @Description(desc = "查询组合工具授权信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        JSONObject resultObj = new JSONObject();
        for (CombopAuthorityAction authorityAction : CombopAuthorityAction.values()) {
            List<String> authorityList = new ArrayList<>();
            List<AutoexecCombopAuthorityVo> authorityVoList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndAction(combopId, authorityAction.getValue());
            for (AutoexecCombopAuthorityVo autoexecCombopAuthorityVo : authorityVoList) {
                authorityList.add(autoexecCombopAuthorityVo.getType() + "#" + autoexecCombopAuthorityVo.getUuid());
            }
            resultObj.put(authorityAction.getValue() + "AuthorityList", authorityList);
        }
        return resultObj;
    }
}
