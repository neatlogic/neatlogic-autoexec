/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.script.paramtype;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeNode extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.NODE.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.NODE.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return $.t("nmaspt.scriptparamtypenode.description");
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 11;
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
                this.put("type", "node");
                this.put("placeholder", $.t("nmaspt.common.placeholder.select"));
            }
        };
    }

    @Override
    protected Object getMyTextByValue(Object value, JSONObject config) {
        JSONArray nodeJsonArray = JSONObject.parseArray(value.toString());
        for (Object node : nodeJsonArray) {
            JSONObject nodeJson = (JSONObject) node;
            nodeJson.put("host", nodeJson.getString("ip"));
        }
        return nodeJsonArray;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        List<AutoexecNodeVo> list = getInputNodeList(jsonArray);
        JSONArray array = new JSONArray(list.size());
        array.addAll(list);
        return array;
    }
}
