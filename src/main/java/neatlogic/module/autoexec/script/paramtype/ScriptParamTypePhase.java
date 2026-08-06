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
import org.springframework.stereotype.Service;

/**
 * @author linbq
 * @since 2022/05/19 17:17
 **/
@Service
public class ScriptParamTypePhase extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.PHASE.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.PHASE.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return $.t("nmaspt.scriptparamtypephase.description");
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 15;
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
                this.put("type", "phase");
                this.put("placeholder", $.t("nmaspt.common.placeholder.select"));
                this.put("multiple", false);
            }
        };
    }

    @Override
    public Boolean myNeedDataSource() {
        return false;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        // 阶段名称，单选
        return getFirstNotBlankString(jsonArray);
    }
}
