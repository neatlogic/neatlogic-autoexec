/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateAuthorityVo;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询组合工具模板授权信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopTemplateAuthorityGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/authority/get";
    }

    @Override
    public String getName() {
        return "查询组合工具模板授权信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopTemplateId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, desc = "编辑授权列表"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, desc = "执行授权列表")
    })
    @Description(desc = "查询组合工具模板授权信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopTemplateId = jsonObj.getLong("combopTemplateId");
        if (autoexecCombopTemplateMapper.checkAutoexecCombopIsExists(combopTemplateId) == 0) {
            throw new AutoexecCombopTemplateNotFoundException(combopTemplateId);
        }
        JSONObject resultObj = new JSONObject();
        for (CombopAuthorityAction authorityAction : CombopAuthorityAction.values()) {
            List<String> authorityList = new ArrayList<>();
            List<AutoexecCombopTemplateAuthorityVo> authorityVoList = autoexecCombopTemplateMapper.getAutoexecCombopAuthorityListByCombopIdAndAction(combopTemplateId, authorityAction.getValue());
            for (AutoexecCombopTemplateAuthorityVo autoexecCombopTemplateAuthorityVo : authorityVoList) {
                authorityList.add(autoexecCombopTemplateAuthorityVo.getType() + "#" + autoexecCombopTemplateAuthorityVo.getUuid());
            }
            resultObj.put(authorityAction.getValue() + "AuthorityList", authorityList);
        }
        return resultObj;
    }
}
