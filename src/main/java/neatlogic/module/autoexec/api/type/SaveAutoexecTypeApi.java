/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.autoexec.api.type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.exception.AutoexecTypeNameRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.service.AutoexecTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveAutoexecTypeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;
    @Resource
    private AutoexecTypeService autoexecTypeService;

    @Override
    public String getToken() {
        return "autoexec/type/save";
    }

    @Override
    public String getName() {
        return "nmaat.saveautoexectypeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, desc = "common.name"),
            @Param(name = "description", type = ApiParamType.STRING, maxLength = 500, desc = "common.description"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "common.authlist"),
            @Param(name = "reviewAuthList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "common.authlist")
    })
    @Output({})
    @Description(desc = "nmaat.saveautoexectypeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecTypeVo typeVo = JSON.toJavaObject(jsonObj, AutoexecTypeVo.class);
        if (autoexecTypeMapper.checkTypeNameIsExists(typeVo) > 0) {
            throw new AutoexecTypeNameRepeatException(typeVo.getName());
        }
        Long id = jsonObj.getLong("id");
        if (id != null) {
            if (autoexecTypeMapper.checkTypeIsExistsById(id) == 0) {
                throw new AutoexecTypeNotFoundException(id);
            }
        }
        autoexecTypeService.saveAutoexecType(typeVo);
//        typeVo.setLcu(UserContext.get().getUserUuid());
//        if (jsonObj.getLong("id") == null) {
//            autoexecTypeMapper.insertType(typeVo);
//        } else {
//            autoexecTypeMapper.deleteTypeAuthByTypeId(typeVo.getId());
//            if (autoexecTypeMapper.checkTypeIsExistsById(typeVo.getId()) == 0) {
//                throw new AutoexecTypeNotFoundException(typeVo.getId());
//            }
//            autoexecTypeMapper.updateType(typeVo);
//        }
//        autoexecTypeMapper.insertTypeAuthList(typeVo.getAutoexecTypeAuthList());
//        JSONArray reviewAuthArray = jsonObj.getJSONArray("reviewAuthList");
//        if (CollectionUtils.isNotEmpty(reviewAuthArray)) {
//            List<AutoexecTypeAuthVo> autoexecTypeAuthList = new ArrayList<>();
//            List<String> reviewAuthList = reviewAuthArray.toJavaList(String.class);
//            for (String reviewAuth : reviewAuthList) {
//                AutoexecTypeAuthVo autoexecTypeAuthVo = convertAutoexecTypeAuthVo(reviewAuth);
//                if (autoexecTypeAuthVo == null) {
//                    continue;
//                }
//                autoexecTypeAuthVo.setTypeId(typeVo.getId());
//                autoexecTypeAuthVo.setAction(AutoexecTypeAuthorityAction.REVIEW.getValue());
//                autoexecTypeAuthList.add(autoexecTypeAuthVo);
//            }
//            autoexecTypeMapper.insertTypeAuthList(autoexecTypeAuthList);
//        }
        return null;
    }

//    private AutoexecTypeAuthVo convertAutoexecTypeAuthVo(String authority) {
//        if (StringUtils.isNotBlank(authority) && authority.contains("#")) {
//            String[] split = authority.split("#");
//            if (GroupSearch.USER.getValue().equals(split[0])) {
//                if (userMapper.checkUserIsExists(split[1]) == 0) {
//                    throw new UserNotFoundException(split[1]);
//                }
//            } else if (GroupSearch.TEAM.getValue().equals(split[0])) {
//                if (teamMapper.checkTeamIsExists(split[1]) == 0) {
//                    throw new TeamNotFoundException(split[1]);
//                }
//            } else if (GroupSearch.ROLE.getValue().equals(split[0])) {
//                if (roleMapper.checkRoleIsExists(split[1]) == 0) {
//                    throw new RoleNotFoundException(split[1]);
//                }
//            } else if (GroupSearch.COMMON.getValue().equals(split[0])) {
//                if (!UserType.ALL.getValue().equals(split[1])) {
//                    return null;
//                }
//            } else {
//                return null;
//            }
//            AutoexecTypeAuthVo autoexecTypeAuthVo = new AutoexecTypeAuthVo();
//            autoexecTypeAuthVo.setAuthType(split[0]);
//            autoexecTypeAuthVo.setAuthUuid(split[1]);
//            return autoexecTypeAuthVo;
//        }
//        return null;
//    }

    public IValid name() {
        return value -> {
            AutoexecTypeVo typeVo = JSON.toJavaObject(value, AutoexecTypeVo.class);
            if (autoexecTypeMapper.checkTypeNameIsExists(typeVo) > 0) {
                return new FieldValidResultVo(new AutoexecTypeNameRepeatException(typeVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
