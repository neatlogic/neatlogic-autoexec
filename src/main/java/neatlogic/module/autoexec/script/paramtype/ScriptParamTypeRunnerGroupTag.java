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
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import neatlogic.framework.dao.mapper.TagMapper;
import neatlogic.framework.dto.TagVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 **/
@Service
public class ScriptParamTypeRunnerGroupTag extends ScriptParamTypeBase {

    @Resource
    TagMapper tagMapper;

    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.RUNNERGROUPTAG.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.RUNNERGROUPTAG.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return $.t("nmaspt.scriptparamtyperunnergrouptag.description");
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
                this.put("type", "runnergrouptag");
                this.put("placeholder", $.t("nmaspt.common.placeholder.select"));
            }
        };
    }

    @Override
    public Object getMyExchangeParamByValue(Object value) {
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            if (value.toString().startsWith("[") && value.toString().startsWith("]")) {
               return value;
            } else {
               return String.format("[\"%s\"]", value);
            }
        }
        return null;
    }

    @Override
    protected Object getMyTextByValue(Object value, JSONObject config) {
        String valueStr = value.toString();
        if (StringUtils.isNotBlank(valueStr)) {
            try {
                TagVo tagVo = tagMapper.getTagById(Long.valueOf(value.toString()));
                if (tagVo != null) {
                    valueStr = tagVo.getName();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return valueStr;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        // 组id，多选
        return getObjectList(jsonArray);
    }
}
