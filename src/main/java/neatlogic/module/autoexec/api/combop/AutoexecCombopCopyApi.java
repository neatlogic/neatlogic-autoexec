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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
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
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.notify.handler.AutoexecCombopNotifyPolicyHandler;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 复制组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_ADD.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopCopyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/copy";
    }

    @Override
    public String getName() {
        return "复制组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "被复制的组合工具id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "新组合工具名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "typeName", type = ApiParamType.STRING, isRequired = true, desc = "类型名"),
            @Param(name = "viewAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "查看权限列表"),
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "编辑权限列表"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "执行权限列表"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "复制组合工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVo fromAutoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (fromAutoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        AutoexecCombopVo autoexecCombopVo = jsonObj.toJavaObject(AutoexecCombopVo.class);
//        Long typeId = jsonObj.getLong("typeId");
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(autoexecCombopVo.getTypeName());
        }
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(config.getInvokeNotifyPolicyConfig(), AutoexecCombopNotifyPolicyHandler.class);
        config.setInvokeNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
//        autoexecCombopVo.setTypeId(typeId);
//        String name = jsonObj.getString("name");
//        autoexecCombopVo.setName(name);
        autoexecCombopVo.setId(null);
        if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            throw new AutoexecCombopNameRepeatException(autoexecCombopVo.getName());
        }
        String userUuid = UserContext.get().getUserUuid(true);
        autoexecCombopVo.setOwner(userUuid);
        autoexecCombopVo.setFcu(userUuid);
        autoexecCombopVo.setOperationType(CombopOperationType.COMBOP.getValue());
        autoexecCombopVo.setIsActive(fromAutoexecCombopVo.getIsActive());
//        autoexecCombopVo.setDescription(jsonObj.getString("description"));
        Long combopId = autoexecCombopVo.getId();
        List<AutoexecCombopVersionVo> versionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(id);
        if (CollectionUtils.isNotEmpty(versionList)) {
            for (AutoexecCombopVersionVo autoexecCombopVersionVo : versionList) {
                autoexecCombopVersionVo.setId(null);
                autoexecCombopVersionVo.setCombopId(combopId);
                AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
                autoexecCombopService.resetIdAutoexecCombopVersionConfig(versionConfig);
                autoexecCombopService.setAutoexecCombopPhaseGroupId(versionConfig);
                autoexecCombopVersionVo.setConfigStr(null);
                autoexecCombopVersionMapper.insertAutoexecCombopVersion(autoexecCombopVersionVo);
                autoexecCombopService.saveDependency(autoexecCombopVersionVo);
            }
        }
        autoexecCombopVo.setConfigStr(null);
        autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
        autoexecCombopService.saveDependency(autoexecCombopVo);
        autoexecCombopService.saveAuthority(autoexecCombopVo);

        return combopId;
    }

    public IValid name() {
        return jsonObj -> {
            String name = jsonObj.getString("name");
            AutoexecCombopVo autoexecCombopVo = new AutoexecCombopVo();
            autoexecCombopVo.setName(name);
            if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopNameRepeatException(autoexecCombopVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
