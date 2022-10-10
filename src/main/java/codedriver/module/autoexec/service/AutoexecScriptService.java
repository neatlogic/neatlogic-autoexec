/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.script.*;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dto.OperateVo;

import java.util.List;
import java.util.Map;

public interface AutoexecScriptService {

    /**
     * 获取脚本版本详细信息，包括参数与脚本内容
     *
     * @param versionId 版本ID
     * @return 脚本版本VO
     */
    AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long versionId);

    /**
     * 获取版本列表的详细信息，包括参数、脚本内容
     *
     * @param vo
     * @return
     */
    List<AutoexecScriptVersionVo> getScriptVersionDetailListByScriptId(AutoexecScriptVersionVo vo);

    /**
     * 校验脚本的基本信息，包括name、uk、分类、操作级别
     *
     * @param scriptVo 脚本VO
     */
    void validateScriptBaseInfo(AutoexecScriptVo scriptVo);

    /**
     * 根据catalogId穿透查询工具目录id
     *
     * @param catalogId
     */
    List<Long> getCatalogIdList(Long catalogId);

    /**
     * 检查脚本内容是否有变更
     *
     * @param before 当前版本
     * @param after  待更新的内容
     * @return 是否有变更
     */
    boolean checkScriptVersionNeedToUpdate(AutoexecScriptVersionVo before, AutoexecScriptVersionVo after);

    /**
     * 获取版本操作列表
     *
     * @param version
     * @return
     */
    List<OperateVo> getOperateListForScriptVersion(AutoexecScriptVersionVo version);

    void saveParamList(Long versionId, List<AutoexecScriptVersionParamVo> newParamList);

    void saveLineList(Long scriptId, Long versionId, List<AutoexecScriptLineVo> lineList);

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

    /**
     * 获取依赖的脚本页面
     *
     * @param map
     * @param groupName
     * @return
     */
    DependencyInfoVo getScriptDependencyPageUrl(Map<String, Object> map, Long scriptId, String groupName);

    /**
     * 根据工具目录路径查询目录ID
     *
     * @param catalogPath 工具目录路径
     * @return 目录ID
     */
    Long getCatalogIdByCatalogPath(String catalogPath);
}
