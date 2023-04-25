/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import com.alibaba.fastjson.JSONObject;

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
     * @param triggerType
     * @param planStartTime
     * @param jobVo
     */
    void settingJobFireMode(String triggerType, Long planStartTime, AutoexecJobVo jobVo) throws Exception;
}
