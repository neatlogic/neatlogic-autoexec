/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存组合工具基本信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopTemplateSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/save";
    }

    @Override
    public String getName() {
        return "保存组合工具模板基本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id"),
//            @Param(name = "uk", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "唯一名"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
//            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id"),
//            @Param(name = "owner", type = ApiParamType.STRING, minLength = 37, maxLength = 37, desc = "维护人"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "保存组合工具模板基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = jsonObj.toJavaObject(AutoexecCombopTemplateVo.class);
        if (autoexecCombopTemplateMapper.checkAutoexecCombopTemplateNameIsRepeat(autoexecCombopTemplateVo) != null) {
            throw new AutoexecCombopNameRepeatException(autoexecCombopTemplateVo.getName());
        }
//        if (autoexecCombopMapper.checkAutoexecCombopUkIsRepeat(autoexecCombopVo) != null) {
//            throw new AutoexecCombopUkRepeatException(autoexecCombopVo.getUk());
//        }
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopTemplateVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(autoexecCombopTemplateVo.getTypeId());
        }
//        if (autoexecCombopTemplateVo.getNotifyPolicyId() != null) {
//            if (notifyMapper.checkNotifyPolicyIsExists(autoexecCombopTemplateVo.getNotifyPolicyId()) == 0) {
//                throw new NotifyPolicyNotFoundException(autoexecCombopTemplateVo.getNotifyPolicyId().toString());
//            }
//        }
        Long id = jsonObj.getLong("id");
        if (id == null) {
//            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
//                throw new PermissionDeniedException(AUTOEXEC_COMBOP_ADD.class);
//            }
            autoexecCombopTemplateVo.setOperationType(CombopOperationType.COMBOP.getValue());
//            autoexecCombopTemplateVo.setOwner(UserContext.get().getUserUuid(true));
            autoexecCombopTemplateVo.setConfig("{}");
            autoexecCombopTemplateMapper.insertAutoexecCombopTemplate(autoexecCombopTemplateVo);
        } else {
//            String owner = autoexecCombopTemplateVo.getOwner();
//            if (owner == null) {
//                throw new ParamNotExistsException("owner");
//            }
//            owner = owner.substring(GroupSearch.USER.getValuePlugin().length());
//            if (userMapper.checkUserIsExists(owner) == 0) {
//                throw new UserNotFoundException(owner);
//            }
//            autoexecCombopTemplateVo.setOwner(owner);
            AutoexecCombopTemplateVo oldAutoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopTemplateById(id);
            if (oldAutoexecCombopTemplateVo == null) {
                throw new AutoexecCombopTemplateNotFoundException(id);
            }
//            autoexecCombopService.setOperableButtonList(oldAutoexecCombopTemplateVo);
//            if (oldAutoexecCombopTemplateVo.getEditable() == 0) {
//                throw new PermissionDeniedException();
//            }
            AutoexecCombopConfigVo config = autoexecCombopTemplateVo.getConfig();
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            List<String> nameList = new ArrayList<>();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                String name = autoexecCombopPhaseVo.getName();
                if (nameList.contains(name)) {
                    throw new AutoexecCombopTemplatePhaseNameRepeatException(name);
                }
                nameList.add(name);
            }
            AutoexecCombopConfigVo oldConfigVo = oldAutoexecCombopTemplateVo.getConfig();
            /** 更新组合工具阶段列表数据时，需要保留执行目标的配置信息 **/
            config.setExecuteConfig(oldConfigVo.getExecuteConfig());
            /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
//            autoexecCombopService.verifyAutoexecCombopConfig(autoexecCombopTemplateVo, false);

//            List<Long> combopPhaseIdList = autoexecCombopTemplateMapper.getCombopPhaseIdListByCombopId(id);
//            if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
//                autoexecCombopTemplateMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
//            }
//            autoexecCombopTemplateMapper.deleteAutoexecCombopPhaseByCombopId(id);
//            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
//                if (autoexecCombopPhaseVo != null) {
//                    autoexecCombopPhaseVo.setCombopId(id);
//                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
//                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
//                    Long combopPhaseId = autoexecCombopPhaseVo.getId();
//                    int jSort = 0;
//                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
//                        if (autoexecCombopPhaseOperationVo != null) {
//                            autoexecCombopPhaseOperationVo.setSort(jSort++);
//                            autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
//                            autoexecCombopTemplateMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
//                        }
//                    }
//                    autoexecCombopTemplateMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
//                }
//            }
            autoexecCombopTemplateMapper.updateAutoexecCombopTemplateById(autoexecCombopTemplateVo);
        }

        return autoexecCombopTemplateVo.getId();
    }

    public IValid name() {
        return jsonObj -> {
            AutoexecCombopTemplateVo autoexecCombopTemplateVo = jsonObj.toJavaObject(AutoexecCombopTemplateVo.class);
            if (autoexecCombopTemplateMapper.checkAutoexecCombopTemplateNameIsRepeat(autoexecCombopTemplateVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopTemplateNameRepeatException(autoexecCombopTemplateVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

//    public IValid uk() {
//        return jsonObj -> {
//            AutoexecCombopVo autoexecCombopVo = JSON.toJavaObject(jsonObj, AutoexecCombopVo.class);
//            if (autoexecCombopMapper.checkAutoexecCombopUkIsRepeat(autoexecCombopVo) != null) {
//                return new FieldValidResultVo(new AutoexecCombopUkRepeatException(autoexecCombopVo.getUk()));
//            }
//            return new FieldValidResultVo();
//        };
//    }
}
