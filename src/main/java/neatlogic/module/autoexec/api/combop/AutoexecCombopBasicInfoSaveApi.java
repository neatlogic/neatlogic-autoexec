/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopNameRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.exception.NotifyPolicyNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopBasicInfoSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

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
        return "autoexec/combop/basic/info/save";
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
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "typeName", type = ApiParamType.STRING, isRequired = true, desc = "类型名"),
            @Param(name = "viewAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "查看权限列表"),
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "编辑权限列表"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "执行权限列表"),
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
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(autoexecCombopVo.getTypeName());
        }
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = config.getInvokeNotifyPolicyConfig();
        if (invokeNotifyPolicyConfigVo != null) {
            Long policyId = invokeNotifyPolicyConfigVo.getPolicyId();
            if (policyId != null) {
                if (notifyMapper.checkNotifyPolicyIsExists(policyId) == 0) {
                    throw new NotifyPolicyNotFoundException(policyId);
                }
                autoexecCombopVo.setNotifyPolicyId(policyId);
            }
        }

        Long id = jsonObj.getLong("id");
        if (id == null) {
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
                throw new PermissionDeniedException(AUTOEXEC_COMBOP_ADD.class);
            }
            autoexecCombopVo.setOperationType(CombopOperationType.COMBOP.getValue());
            autoexecCombopVo.setOwner(UserContext.get().getUserUuid(true));
            autoexecCombopVo.setIsActive(0);
            autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
            autoexecCombopService.saveDependency(autoexecCombopVo);
            autoexecCombopService.saveAuthority(autoexecCombopVo);
            // 创建一个新的组合工具的同时创建一个空草稿版本
            AutoexecCombopVersionVo autoexecCombopVersionVo = new AutoexecCombopVersionVo();
            autoexecCombopVersionVo.setCombopId(autoexecCombopVo.getId());
            autoexecCombopVersionVo.setVersion(1);
            autoexecCombopVersionVo.setStatus(ScriptVersionStatus.DRAFT.getValue());
            autoexecCombopVersionVo.setIsActive(0);
            autoexecCombopVersionVo.setName(autoexecCombopVo.getName());
            autoexecCombopVersionVo.setConfig(new AutoexecCombopVersionConfigVo());
            autoexecCombopVersionVo.setLcu(UserContext.get().getUserUuid());
            autoexecCombopVersionMapper.insertAutoexecCombopVersion(autoexecCombopVersionVo);
        } else {
            String owner = autoexecCombopVo.getOwner();
            if (owner == null) {
                throw new ParamNotExistsException("维护人（owner）");
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
            autoexecCombopService.deleteDependency(oldAutoexecCombopVo);
            autoexecCombopVo.setConfigStr(null);
            autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
            autoexecCombopService.saveDependency(autoexecCombopVo);
            autoexecCombopMapper.deleteAutoexecCombopAuthorityByCombopId(id);
            autoexecCombopService.saveAuthority(autoexecCombopVo);
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
}
