/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateParamVo;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 查询组合工具模板顶层参数列表接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopTemplateParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;
    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/param/list";
    }

    @Override
    public String getName() {
        return "查询组合工具模板顶层参数列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopTemplateId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具模板模板id")
    })
    @Output({
            @Param(explode = AutoexecCombopTemplateParamVo[].class, desc = "参数列表")
    })
    @Description(desc = "查询组合工具模板顶层参数列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopTemplateId = jsonObj.getLong("combopTemplateId");
        if (autoexecCombopTemplateMapper.checkAutoexecCombopIsExists(combopTemplateId) == 0) {
            throw new AutoexecCombopTemplateNotFoundException(combopTemplateId);
        }
        List<AutoexecCombopTemplateParamVo> autoexecCombopTemplateParamVoList = autoexecCombopTemplateMapper.getAutoexecCombopParamListByCombopId(combopTemplateId);
        for (AutoexecCombopTemplateParamVo autoexecCombopTemplateParamVo : autoexecCombopTemplateParamVoList) {
            autoexecService.mergeConfig(autoexecCombopTemplateParamVo);
        }
        return autoexecCombopTemplateParamVoList;
    }
}
