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

package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.module.autoexec.process.dto.AutoexecJobBuilder;

public interface AutoexecServiceService {
    /**
     * 检测服务配置信息是否已失效，如果失效，则返回失效原因
     * @param serviceVo 服务信息
     * @param throwException 是否抛异常，不抛异常就记录日志
     * @return 失效原因列表
     */
    JSONArray checkConfigExpired(AutoexecServiceVo serviceVo, boolean throwException);

    /**
     * 根据配置信息创建AutoexecJobBuilder对象
     *
     * @param autoexecServiceVo
     * @param autoexecCombopVersionVo
     * @param name
     * @param scenarioId
     * @param formAttributeDataList
     * @param hidecomponentList
     * @param roundCount
     * @param executeUser
     * @param protocol
     * @param executeNodeConfig
     * @param runtimeParamMap
     * @param runnerGroup
     * @param runnerGroupTag
     * @return
     */
    AutoexecJobBuilder getAutoexecJobBuilder(
            AutoexecServiceVo autoexecServiceVo,
            AutoexecCombopVersionVo autoexecCombopVersionVo,
            String name,
            Long scenarioId,
            JSONArray formAttributeDataList,
            JSONArray hidecomponentList,
            Integer roundCount,
            Integer parallelCount,
            String parallelPolicy,
            String executeUser,
            Long protocol,
            AutoexecCombopExecuteNodeConfigVo executeNodeConfig,
            JSONObject runtimeParamMap,
            ParamMappingVo runnerGroup,
            ParamMappingVo runnerGroupTag);
}
