/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.UserMapper;
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
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.exception.AutoexecCombopNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecCombopUkRepeatException;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 保存组合工具基本信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
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
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id"),
            @Param(name = "owner", type = ApiParamType.STRING, minLength = 37, maxLength = 37, desc = "维护人"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "保存组合工具基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVo autoexecCombopVo = JSON.toJavaObject(jsonObj, AutoexecCombopVo.class);
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
            autoexecCombopVo.setOperationType(CombopOperationType.COMBOP.getValue());
            autoexecCombopVo.setOwner(UserContext.get().getUserUuid(true));
            autoexecCombopVo.setConfig("{}");
            autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
        } else {
            String owner = autoexecCombopVo.getOwner();
            if (owner == null) {
                throw new ParamNotExistsException("参数：“owner（维护人）”不能为空");
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
            if (!Objects.equals(owner, oldAutoexecCombopVo.getOwner())) {
                if (oldAutoexecCombopVo.getOwnerEditable() == 0) {
                    throw new PermissionDeniedException();
                }
            }
            AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
            AutoexecCombopConfigVo oldConfigVo = oldAutoexecCombopVo.getConfig();
            /** 更新组合工具阶段列表数据时，需要保留执行目标的配置信息 **/
            config.setExecuteConfig(oldConfigVo.getExecuteConfig());
            /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
            autoexecCombopService.verifyAutoexecCombopConfig(autoexecCombopVo);
            List<Long> combopPhaseIdList = autoexecCombopMapper.getCombopPhaseIdListByCombopId(id);

            if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
                autoexecCombopMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
            }
            autoexecCombopMapper.deleteAutoexecCombopPhaseByCombopId(id);
            int iSort = 0;
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo != null) {
                    autoexecCombopPhaseVo.setCombopId(id);
                    autoexecCombopPhaseVo.setSort(iSort++);
                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                    Long combopPhaseId = autoexecCombopPhaseVo.getId();
                    int jSort = 0;
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        if (autoexecCombopPhaseOperationVo != null) {
                            autoexecCombopPhaseOperationVo.setSort(jSort++);
                            autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
                            autoexecCombopMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
                        }
                    }
                    autoexecCombopMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
                }
            }
            autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
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
}
