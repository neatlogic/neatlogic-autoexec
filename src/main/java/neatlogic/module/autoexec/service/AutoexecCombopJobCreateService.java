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

import com.alibaba.fastjson.JSONObject;
import neatlogic.module.autoexec.dto.job.AutoexecCombopJobBuildResultVo;

public interface AutoexecCombopJobCreateService {

    /**
     * 按公共创建接口的既有规则初始化作业发起用户上下文。
     *
     * @param execUserUuid 指定的用户UUID，为空时使用当前用户
     * @return 最终作业发起用户UUID
     */
    String initExecUserContext(String execUserUuid);

    /**
     * 按公共创建接口的既有规则，根据组合工具活动版本构建作业。
     * 本方法只构建参数，不保存也不激活作业。
     *
     * @param paramObj     已完成接口边界适配的创建参数
     * @param execUserUuid 作业发起用户UUID
     * @return 作业及本次解析到的活动版本ID
     */
    AutoexecCombopJobBuildResultVo buildJob(JSONObject paramObj, String execUserUuid);
}
