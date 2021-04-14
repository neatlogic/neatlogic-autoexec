/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.AutoexecScriptVo;

public interface AutoexecScriptService {

    /**
     * 获取脚本版本详细信息，包括参数与脚本内容
     *
     * @param versionId 版本ID
     * @return 脚本版本VO
     */
    public AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long versionId);

    /**
     * 校验脚本的基本信息，包括name、label、分类、操作级别
     * @param scriptVo 脚本VO
     */
    public void validateScriptBaseInfo(AutoexecScriptVo scriptVo);

}