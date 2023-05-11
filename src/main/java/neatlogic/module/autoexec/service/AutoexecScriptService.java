/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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
}
