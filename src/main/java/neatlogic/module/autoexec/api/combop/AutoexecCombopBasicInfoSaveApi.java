/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNameRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.notify.handler.AutoexecCombopNotifyPolicyHandler;
import neatlogic.module.autoexec.service.AutoexecCombopService;
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
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/basic/info/save";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccombopbasicinfosaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "common.name"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "common.typeid"),
            @Param(name = "opType", type = ApiParamType.STRING, isRequired = true, desc = "common.actiontype"),
            @Param(name = "typeName", type = ApiParamType.STRING, isRequired = true, desc = "common.typename"),
            @Param(name = "viewAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "common.viewauthoritylist"),
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "common.editauthoritylist"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "common.executeauthoritylist"),
            @Param(name = "owner", type = ApiParamType.STRING, minLength = 37, maxLength = 37, desc = "common.owneruuid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "common.id")
    })
    @Description(desc = "nmaac.autoexeccombopbasicinfosaveapi.getname")
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
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(config.getInvokeNotifyPolicyConfig(), AutoexecCombopNotifyPolicyHandler.class);
        config.setInvokeNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
        Long id = jsonObj.getLong("id");
        if (id == null) {
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
                throw new PermissionDeniedException(AUTOEXEC_COMBOP_ADD.class);
            }
            autoexecCombopVo.setOperationType(CombopOperationType.COMBOP.getValue());
            autoexecCombopVo.setOwner(UserContext.get().getUserUuid(true));
            autoexecCombopVo.setIsActive(0);
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
        }
        autoexecCombopService.saveAutoexecCombop(autoexecCombopVo);
        if (id == null) {
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
