/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;

public interface AutoexecServiceService {
    /**
     * 检测服务配置信息是否已失效，如果失效，则返回失效原因
     * @param serviceVo 服务信息
     * @param throwException 是否抛异常，不抛异常就记录日志
     * @return 失效原因列表
     */
    JSONArray checkConfigExpired(AutoexecServiceVo serviceVo, boolean throwException);
}
