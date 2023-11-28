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

package neatlogic.module.autoexec.script.paramtype;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
public class ScriptParamTypeUserSelect extends ScriptParamTypeBase {

    @Resource
    UserMapper userMapper;

    @Resource
    TeamMapper teamMapper;

    @Resource
    RoleMapper roleMapper;

    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.USERSELECT.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.USERSELECT.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "用于选择系统用户、分组、角色";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 13;
    }

    /**
     * 获取前端初始化配置
     *
     * @return 配置
     */
    @Override
    public JSONObject getConfig() {
        return new JSONObject() {
            {
                this.put("type", "userselect");
                this.put("placeholder", "请选择");
            }
        };
    }

    @Override
    public Object getMyTextByValue(Object value, JSONObject config) {
        String valueString = value.toString();
        if (valueString.startsWith("[") && valueString.endsWith("]")) {
            return JSONObject.parseArray(valueString);
        }
        return value;
    }

    @Override
    public Object getMyExchangeParamByValue(Object value) {
        String valueString = value.toString();
        if (valueString.startsWith("[") && valueString.endsWith("]")) {
            JSONArray result = new JSONArray();
            JSONArray valueArray = JSONObject.parseArray(valueString);
            for (int i = 0; i < valueArray.size(); i++) {
                result.add(getGroupSearch(valueArray.getString(i)));
            }
            return result;
        } else {
            return getGroupSearch(valueString);
        }
    }

    /**
     * 将userId->userUuid,teamName->teamUuid,roleName->roleUuid
     *
     * @param valueStr 待转换的值
     * @return 转换后的值
     */
    private String getGroupSearch(String valueStr) {
        if (Objects.equals(GroupSearch.USER.getValue(), GroupSearch.getPrefix(valueStr))) {
            UserVo userVo = userMapper.getUserByUserId(GroupSearch.removePrefix(valueStr));
            if (userVo != null) {
                return GroupSearch.USER.getValuePlugin() + userVo.getUuid();
            } else {
                return valueStr;
            }
        } else if (Objects.equals(GroupSearch.TEAM.getValue(), GroupSearch.getPrefix(valueStr))) {
            List<String> teamUuidList = teamMapper.getTeamUuidByName(GroupSearch.removePrefix(valueStr));
            if (CollectionUtils.isNotEmpty(teamUuidList) && teamUuidList.size() == 1) {
                return GroupSearch.TEAM.getValuePlugin() + teamUuidList.get(0);
            } else {
                return valueStr;
            }
        } else if (Objects.equals(GroupSearch.ROLE.getValue(), GroupSearch.getPrefix(valueStr))) {
            List<String> roleUuidList = roleMapper.getRoleUuidByName(GroupSearch.removePrefix(valueStr));
            if (CollectionUtils.isNotEmpty(roleUuidList) && roleUuidList.size() == 1) {
                return GroupSearch.ROLE.getValuePlugin() + roleUuidList.get(0);
            } else {
                return valueStr;
            }
        }
        return valueStr;
    }
}
