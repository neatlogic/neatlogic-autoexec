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
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeFile extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.FILE.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.FILE.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "支持多个文件同时上传，执行时，自动上传文件到目标主机特定目录下，保留原文件名";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 2;
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
                this.put("type", "file");
                this.put("dataType", "autoexec");
                this.put("formatList", new ArrayList<>());
                this.put("placeholder", "请上传");
            }
        };
    }

    @Override
    protected Object getMyTextByValue(Object value, JSONObject config) {
        return JSONObject.parseObject(value.toString());
    }

    @Override
    protected Object getMyAutoexecParamByValue(Object value) {
        value = JSONObject.parseObject(value.toString()).getJSONArray("fileIdList");
        return value;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        // 多选
        return getFileInfo(jsonArray);
    }
}
