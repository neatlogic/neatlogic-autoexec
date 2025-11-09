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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import neatlogic.framework.common.util.RC4Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypePassword extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.PASSWORD.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.PASSWORD.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "可输入数字或字符串，页面显示为密文";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 1;
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
                this.put("type", "password");
//                this.put("maxlength", 50);
                this.put("showPassword", true);
                this.put("placeholder", "请输入");
            }
        };
    }

    @Override
    protected Object getMyTextByValue(Object value, JSONObject config) {
        String valueStr = value.toString();
        if (StringUtils.isNotBlank(valueStr)) {
            return RC4Util.encrypt(valueStr);
        }
        return value;
    }

    @Override
    public Object getMyAutoexecParamByValue(Object value){
        return getMyTextByValue(value, null);
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        return getString(jsonArray);
    }
}
