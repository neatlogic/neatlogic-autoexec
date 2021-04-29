/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.script.*;

import java.util.List;

public interface AutoexecScriptService {

    /**
     * 获取脚本版本详细信息，包括参数与脚本内容
     *
     * @param versionId 版本ID
     * @return 脚本版本VO
     */
    AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long versionId);

    /**
     * 根据脚本ID获取所有版本的详细信息，包括参数、脚本内容
     *
     * @param scriptId
     * @return
     */
    List<AutoexecScriptVersionVo> getScriptVersionDetailListByScriptId(Long scriptId);

    /**
     * 校验脚本的基本信息，包括name、uk、分类、操作级别
     *
     * @param scriptVo 脚本VO
     */
    void validateScriptBaseInfo(AutoexecScriptVo scriptVo);

    /**
     * 批量插入脚本参数
     *
     * @param paramList 参数列表
     * @param batchSize 每批的数量
     */
    void batchInsertScriptVersionParamList(List<AutoexecScriptVersionParamVo> paramList, int batchSize);

    /**
     * 批量插入脚本内容行
     *
     * @param lineList  内容行列表
     * @param batchSize 每批的数量
     */
    void batchInsertScriptLineList(List<AutoexecScriptLineVo> lineList, int batchSize);

    /**
     * 记录活动
     *
     * @param auditVo 活动VO
     */
    void audit(AutoexecScriptAuditVo auditVo);

}
