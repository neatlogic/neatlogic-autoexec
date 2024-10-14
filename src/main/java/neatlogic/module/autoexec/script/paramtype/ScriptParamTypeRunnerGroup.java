/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2024/04/08 15:37
 **/
@Service
public class ScriptParamTypeRunnerGroup extends ScriptParamTypeBase {

    @Resource
    RunnerMapper runnerMapper;

    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.RUNNERGROUP.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.RUNNERGROUP.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "执行器组";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 99;
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
                this.put("type", "runnergroup");
                this.put("placeholder", "请选择");
            }
        };
    }

    @Override
    public Object getMyExchangeParamByValue(Object value) {
        Long runnerGroupId = null;
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            try {
                runnerGroupId = Long.valueOf(value.toString());
            } catch (NumberFormatException ignored) {
            }
            if (runnerGroupId == null) {
                RunnerGroupVo runnerGroupVo = runnerMapper.getRunnerGroupByName(value.toString());
                if (runnerGroupVo != null) {
                    runnerGroupId = runnerGroupVo.getId();
                }
            }

        }
        return runnerGroupId;
    }

    @Override
    protected Object getMyTextByValue(Object value, JSONObject config) {
        String valueStr = value.toString();
        if (StringUtils.isNotBlank(valueStr)) {
            try {
                RunnerGroupVo runnerGroupVo = runnerMapper.getRunnerGroupById(Long.valueOf(value.toString()));
                if (runnerGroupVo != null) {
                    valueStr = runnerGroupVo.getName();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return valueStr;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        // 组id，单选
        return getFirstNotNullObject(jsonArray);
    }
}
