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

import neatlogic.framework.autoexec.dto.script.*;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dto.OperateVo;

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

    /** Writes base information with directory/name uniqueness, preserving the caller's insert/update intent. */
    void persistScriptBaseInfo(AutoexecScriptVo scriptVo, boolean insert);


    /** Resolves a name within an optional full catalog path; missing tools return null, ambiguous names fail. */
    AutoexecScriptVo resolveScriptByName(String name, String fullCatalogName);

    /** Resolves one reference from a batch of candidates without additional database queries. */
    AutoexecScriptVo resolveScriptByName(String name, String fullCatalogName, List<AutoexecScriptVo> candidates);

    /** Resolves legacy full-path library references in one batch, preserving the reference as the map key. */
    Map<String, AutoexecScriptVo> resolveScriptByPathList(List<String> references);


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

    /**
     * 根据工具目录路径层层查询目录ID，如果目录不存在则创建
     *
     * @param catalogPath 工具目录路径
     * @return 目录ID
     */
    Long createCatalogByCatalogPath(String catalogPath);

    /**
     * 根据脚本ID删除脚本
     *
     * @param id 脚本ID
     */
    void deleteScriptById(Long id);

    /**
     * 保存自定义工具基本信息
     * @param scriptVo
     */
    void saveScript(AutoexecScriptVo scriptVo);

    /**
     * 保存自定义基本信息和版本信息
     * @param scriptVo
     * @param versionVo
     */
    void saveScriptAndVersion(AutoexecScriptVo scriptVo, AutoexecScriptVersionVo versionVo);

    /**
     * 审批版本
     * @param version
     * @param action
     * @param content
     */
    void reviewVersion(AutoexecScriptVersionVo version, String action, String content);
}
