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
import neatlogic.module.autoexec.dto.job.AutoexecJobSyncResultVo;

public interface AutoexecCombopJobSyncService {

    /**
     * 创建并立即执行组合工具作业，等待作业终止或监听超时后返回。
     *
     * @param paramObj     公共创建接口参数
     * @param execUserUuid 当前认证用户UUID
     * @return 同步作业结果
     */
    AutoexecJobSyncResultVo createAndWait(JSONObject paramObj, String execUserUuid) throws Exception;
}
