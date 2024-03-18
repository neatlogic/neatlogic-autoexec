/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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
