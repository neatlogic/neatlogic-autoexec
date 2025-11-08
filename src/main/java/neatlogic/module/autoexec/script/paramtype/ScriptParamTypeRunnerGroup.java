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
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.exception.runner.RunnerGroupNotFoundException;
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
                }else{
                    throw new RunnerGroupNotFoundException(value.toString());
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
