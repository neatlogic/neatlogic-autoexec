/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.exception.user.UserNotFoundException;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.exception.NotifyPolicyNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.module.autoexec.dependency.AutoexecGlobalParam2CombopPhaseOperationDependencyHandler;
import codedriver.module.autoexec.dependency.AutoexecProfile2CombopPhaseOperationDependencyHandler;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 保存组合工具基本信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private NotifyMapper notifyMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/save";
    }

    @Override
    public String getName() {
        return "保存组合工具基本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id"),
//            @Param(name = "uk", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "唯一名"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id"),
            @Param(name = "combopTemplateId", type = ApiParamType.LONG, desc = "组合工具模板id"),
            @Param(name = "owner", type = ApiParamType.STRING, minLength = 37, maxLength = 37, desc = "维护人"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "保存组合工具基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVo autoexecCombopVo = jsonObj.toJavaObject(AutoexecCombopVo.class);
        if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            throw new AutoexecCombopNameRepeatException(autoexecCombopVo.getName());
        }
//        if (autoexecCombopMapper.checkAutoexecCombopUkIsRepeat(autoexecCombopVo) != null) {
//            throw new AutoexecCombopUkRepeatException(autoexecCombopVo.getUk());
//        }
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(autoexecCombopVo.getTypeId());
        }
        if (autoexecCombopVo.getNotifyPolicyId() != null) {
            if (notifyMapper.checkNotifyPolicyIsExists(autoexecCombopVo.getNotifyPolicyId()) == 0) {
                throw new NotifyPolicyNotFoundException(autoexecCombopVo.getNotifyPolicyId().toString());
            }
        }

        Long id = jsonObj.getLong("id");
        if (id == null) {
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
                throw new PermissionDeniedException(AUTOEXEC_COMBOP_ADD.class);
            }
            autoexecCombopVo.setOperationType(CombopOperationType.COMBOP.getValue());
            autoexecCombopVo.setOwner(UserContext.get().getUserUuid(true));
            autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
            saveDependency(autoexecCombopVo);
        } else {
            String owner = autoexecCombopVo.getOwner();
            if (owner == null) {
                throw new ParamNotExistsException("owner");
            }
            owner = owner.substring(GroupSearch.USER.getValuePlugin().length());
            if (userMapper.checkUserIsExists(owner) == 0) {
                throw new UserNotFoundException(owner);
            }
            autoexecCombopVo.setOwner(owner);
            AutoexecCombopVo oldAutoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
            if (oldAutoexecCombopVo == null) {
                throw new AutoexecCombopNotFoundException(id);
            }
            autoexecCombopService.setOperableButtonList(oldAutoexecCombopVo);
            if (oldAutoexecCombopVo.getEditable() == 0) {
                throw new PermissionDeniedException();
            }
            AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            List<String> nameList = new ArrayList<>();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                String name = autoexecCombopPhaseVo.getName();
                if (nameList.contains(name)) {
                    throw new AutoexecCombopPhaseNameRepeatException(name);
                }
                nameList.add(name);
            }
            AutoexecCombopConfigVo oldConfigVo = oldAutoexecCombopVo.getConfig();
            /** 更新组合工具阶段列表数据时，需要保留执行目标的配置信息 **/
            config.setExecuteConfig(oldConfigVo.getExecuteConfig());
            /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
            autoexecCombopService.verifyAutoexecCombopConfig(autoexecCombopVo, false);

            List<Long> combopPhaseIdList = autoexecCombopMapper.getCombopPhaseIdListByCombopId(id);

            if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
                autoexecCombopMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
            }
            autoexecCombopMapper.deleteAutoexecCombopPhaseByCombopId(id);
            autoexecCombopMapper.deleteAutoexecCombopGroupByCombopId(id);
            deleteDependency(oldAutoexecCombopVo);
            autoexecCombopService.saveAutoexecCombopConfig(autoexecCombopVo, false);
            autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
            saveDependency(autoexecCombopVo);
        }

        return autoexecCombopVo.getId();
    }

    public IValid name() {
        return jsonObj -> {
            AutoexecCombopVo autoexecCombopVo = JSON.toJavaObject(jsonObj, AutoexecCombopVo.class);
            if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopNameRepeatException(autoexecCombopVo.getName()));
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

    private void saveDependency(AutoexecCombopVo autoexecCombopVo) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                return;
            }
            AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
            if (phaseConfig == null) {
                return;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                return;
            }
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                if (autoexecCombopPhaseOperationVo != null) {
                    saveDependency(autoexecCombopPhaseVo, autoexecCombopPhaseOperationVo);
                }
            }
        }
    }

    private void saveDependency(AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo) {
        AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
        if (operationConfigVo == null) {
            return;
        }
        Long profileId = operationConfigVo.getProfileId();
        if (profileId != null) {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("combopId", combopPhaseVo.getId());
            dependencyConfig.put("combopName", combopPhaseVo.getName());
            dependencyConfig.put("phaseUuid", combopPhaseVo.getUuid());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            DependencyManager.insert(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getOperationId(), dependencyConfig);
        }
        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("combopId", combopPhaseVo.getId());
                    dependencyConfig.put("combopName", combopPhaseVo.getName());
                    dependencyConfig.put("phaseUuid", combopPhaseVo.getUuid());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    dependencyConfig.put("type", "输入参数映射");
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getOperationId(), dependencyConfig);
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("combopId", combopPhaseVo.getId());
                    dependencyConfig.put("combopName", combopPhaseVo.getName());
                    dependencyConfig.put("phaseUuid", combopPhaseVo.getUuid());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    dependencyConfig.put("type", "自由参数映射");
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getOperationId(), dependencyConfig);
                }
            }
        }
    }

    private void deleteDependency(AutoexecCombopVo autoexecCombopVo) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                return;
            }
            AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
            if (phaseConfig == null) {
                return;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                return;
            }
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo != null) {
                    DependencyManager.delete(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, phaseOperationVo.getOperationId());
                    DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationDependencyHandler.class, phaseOperationVo.getOperationId());
                }
            }
        }
    }
}
