/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;

/**
 * @author lvzk
 * @since 2021/4/27 11:29
 **/
public interface AutoexecJobActionService {

    /**
     * 拼装给runner的param
     *
     * @param paramJson 返回param值
     * @param jobVo     作业
     */
    void getFireParamJson(JSONObject paramJson, AutoexecJobVo jobVo);

    /**
     * 校验根据组合工具创建的作业
     *
     */
    void validateAndCreateJobFromCombop(AutoexecJobVo autoexecJobParam) throws Exception;

    /**
     * 校验创建并激活作业
     *
     */
    void validateCreateJob(AutoexecJobVo autoexecJobParam) throws Exception;

    /**
     * 补充job详细信息并fire job
     *
     * @param jobVo AutoexecJobVo
     * @throws Exception
     */
    void getJobDetailAndFireJob(AutoexecJobVo jobVo) throws Exception;


    /**
     * 初始化执行用户上下文
     * @param jobVo 作业
     * @throws Exception 异常
     */
    void initExecuteUserContext(AutoexecJobVo jobVo) throws Exception;

    /**
     * 设置作业的激活方式
     * @param jobVo 作业
     */
    void settingJobFireMode(AutoexecJobVo jobVo) throws Exception;
}
