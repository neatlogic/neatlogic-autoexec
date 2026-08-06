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
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.service.IFormCrossoverService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeCheckbox extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.CHECKBOX.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.CHECKBOX.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return $.t("nmaspt.scriptparamtypecheckbox.description");
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 10;
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
                this.put("type", "checkbox");
                this.put("placeholder", $.t("nmaspt.common.placeholder.select"));
            }
        };
    }

    @Override
    public Boolean myNeedDataSource(){
        return true;
    }

    @Override
    public Object getMyTextByValue(Object value, JSONObject config) {
        IFormCrossoverService formCrossoverService = CrossoverServiceFactory.getApi(IFormCrossoverService.class);
        AttributeDataVo attributeDataVo = new AttributeDataVo();
        attributeDataVo.setDataObj(value);
        JSONObject resultObj = formCrossoverService.getMyDetailedDataForSelectHandler(attributeDataVo, config);
        JSONArray textList = resultObj.getJSONArray("textList");
        if (CollectionUtils.isNotEmpty(textList)) {
            return textList;
        }
        return value;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        return getObjectList(jsonArray);
    }
}
