/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;

public interface AutoexecServiceService {
    /**
     * 检测服务配置信息是否已失效，如果失效，则返回失效原因
     * @param serviceVo 服务信息
     * @param throwException 是否抛异常，不抛异常就记录日志
     * @return 失效原因
     */
    String checkConfigExpired(AutoexecServiceVo serviceVo, boolean throwException);
}
