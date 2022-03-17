/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateParamVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 复制组合工具模板接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopTemplateCopyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/copy";
    }

    @Override
    public String getName() {
        return "复制组合工具模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "被复制的组合工具模板id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "新组合工具模板名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "复制组合工具模板")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopById(id);
        if (autoexecCombopTemplateVo == null) {
            throw new AutoexecCombopTemplateNotFoundException(id);
        }
        Long typeId = jsonObj.getLong("typeId");
        if (autoexecTypeMapper.checkTypeIsExistsById(typeId) == 0) {
            throw new AutoexecTypeNotFoundException(typeId);
        }
        autoexecCombopTemplateVo.setTypeId(typeId);
        String name = jsonObj.getString("name");
        autoexecCombopTemplateVo.setName(name);
        autoexecCombopTemplateVo.setId(null);
        if (autoexecCombopTemplateMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopTemplateVo) != null) {
            throw new AutoexecCombopTemplateNameRepeatException(autoexecCombopTemplateVo.getName());
        }
        String userUuid = UserContext.get().getUserUuid(true);
//        autoexecCombopTemplateVo.setOwner(userUuid);
        autoexecCombopTemplateVo.setFcu(userUuid);
        autoexecCombopTemplateVo.setOperationType(CombopOperationType.COMBOP.getValue());
        autoexecCombopTemplateVo.setDescription(jsonObj.getString("description"));
        AutoexecCombopConfigVo config = autoexecCombopTemplateVo.getConfig();
        Long combopTemplateId = autoexecCombopTemplateVo.getId();
//        int iSort = 0;
//        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
//        if(CollectionUtils.isNotEmpty(combopPhaseList)){
//            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
//                if (autoexecCombopPhaseVo != null) {
//                    autoexecCombopPhaseVo.setId(null);
//                    autoexecCombopPhaseVo.setCombopId(combopId);
//                    autoexecCombopPhaseVo.setSort(iSort++);
//                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
//                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
//                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
//                        Long combopPhaseId = autoexecCombopPhaseVo.getId();
//                        int jSort = 0;
//                        for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
//                            if (autoexecCombopPhaseOperationVo != null) {
//                                autoexecCombopPhaseOperationVo.setSort(jSort++);
//                                autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
//                                autoexecCombopTemplateMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
//                            }
//                        }
//                    }
//                    autoexecCombopTemplateMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
//                }
//            }
//        }
        autoexecCombopTemplateMapper.insertAutoexecCombop(autoexecCombopTemplateVo);

        List<AutoexecCombopTemplateParamVo> runtimeParamList = autoexecCombopTemplateMapper.getAutoexecCombopParamListByCombopId(id);
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            for (AutoexecCombopTemplateParamVo autoexecCombopTemplateParamVo : runtimeParamList) {
                autoexecCombopTemplateParamVo.setCombopTemplateId(combopTemplateId);
            }
            autoexecCombopTemplateMapper.insertAutoexecCombopParamVoList(runtimeParamList);
        }
        return combopTemplateId;
    }

    public IValid name() {
        return jsonObj -> {
            String name = jsonObj.getString("name");
            AutoexecCombopTemplateVo autoexecCombopTemplateVo = new AutoexecCombopTemplateVo();
            autoexecCombopTemplateVo.setName(name);
            if (autoexecCombopTemplateMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopTemplateVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopTemplateNameRepeatException(autoexecCombopTemplateVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
