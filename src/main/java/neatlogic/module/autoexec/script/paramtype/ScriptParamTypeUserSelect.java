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
