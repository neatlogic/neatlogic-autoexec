/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeSwitch extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.SWITCH.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.SWITCH.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "开关";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 16;
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
                this.put("type", "switch");
            }
        };
    }

    @Override
    public Boolean myNeedDataSource() {
        return false;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        // true或false
        Boolean bool = getFirstNotNullBoolean(jsonArray);
        if (Boolean.TRUE == bool) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }
}
