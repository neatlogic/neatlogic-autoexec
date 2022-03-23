/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
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
     * @param isNeedAuth 是否需要鉴权
     */
    AutoexecJobVo validateCreateJobFromCombop(JSONObject param,boolean isNeedAuth);

    /**
     * 校验创建并激活作业
     * @param isNeedAuth 是否需要鉴权
     */
    void validateCreateJob(JSONObject param,boolean isNeedAuth) throws Exception;
}
