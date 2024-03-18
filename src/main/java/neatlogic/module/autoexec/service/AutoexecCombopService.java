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

import neatlogic.framework.autoexec.constvalue.CombopAuthorityAction;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;

import java.util.List;

/**
 * @author: linbq
 * @date: 2021/4/15 16:13
 **/
public interface AutoexecCombopService {

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    void setOperableButtonList(AutoexecCombopVo autoexecCombopVo);

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param combopVoList 组合工具Vo对象列表
     */
    void setOperableButtonList(List<AutoexecCombopVo> combopVoList);

    /**
     * 检查当前用户是否有当前组合工具的某项权限
     *
     * @param autoexecCombopVo 组合工具
     * @param action           权限
     * @return 是｜否
     */
    boolean checkOperableButton(AutoexecCombopVo autoexecCombopVo, CombopAuthorityAction action);

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param autoexecCombopConfigVo 组合工具Vo对象配置信息
     * @param isExecuteJob           是否执行创建作业
     * @return 是否合法
     */
    boolean verifyAutoexecCombopConfig(AutoexecCombopConfigVo autoexecCombopConfigVo, boolean isExecuteJob);

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param autoexecCombopVersionConfigVo 组合工具版本对象配置信息
     * @param isExecuteJob           是否执行创建作业
     * @return 是否合法
     */
    boolean verifyAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo autoexecCombopVersionConfigVo, boolean isExecuteJob);

    /**
     * 通过操作id 获取当前激活版本脚本内容
     *
     * @param operation 操作Id
     * @return 脚本内容
     */
    String getScriptVersionContent(AutoexecScriptVersionVo operation);

    String getOperationActiveVersionScriptByOperationId(Long operationId);

    /**
     * 判断是否需要设置执行目标、执行用户、连接协议
     *
     * @param autoexecCombopVersionVo      组合工具版本信息
     * @param autoexecCombopPhaseVo 阶段信息
     * @param autoexecCombopGroupVo 组信息
     */
    void needExecuteConfig(AutoexecCombopVersionVo autoexecCombopVersionVo, AutoexecCombopPhaseVo autoexecCombopPhaseVo, AutoexecCombopGroupVo autoexecCombopGroupVo);
    /**
     * 判断是否需要设置执行目标、执行用户、连接协议、分批数量
     *
     * @param autoexecCombopVersionVo      组合工具版本信息
     */
    void needExecuteConfig(AutoexecCombopVersionVo autoexecCombopVersionVo);

    /**
     * 设置组合工具版本配置信息的阶段中groupId
     *
     * @param autoexecCombopVersionConfigVo
     */
    void setAutoexecCombopPhaseGroupId(AutoexecCombopVersionConfigVo autoexecCombopVersionConfigVo);

    /**
     * 重置组合工具版本配置信息中各种ID，如阶段ID，操作ID
     *
     * @param autoexecCombopVersionConfigVo
     */
    void resetIdAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo autoexecCombopVersionConfigVo);

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVo
     */
    void saveDependency(AutoexecCombopVo autoexecCombopVo);

    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVo
     */
    void deleteDependency(AutoexecCombopVo autoexecCombopVo);

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVersionVo
     */
    void saveDependency(AutoexecCombopVersionVo autoexecCombopVersionVo);

    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVersionVo
     */
    void deleteDependency(AutoexecCombopVersionVo autoexecCombopVersionVo);

    /**
     * 获取组合工具版本信息
     *
     * @param id
     * @return
     */
    AutoexecCombopVersionVo getAutoexecCombopVersionById(Long id);

    /**
     * 根据protocolId补充protocol字段和protocolPort字段值
     * @param autoexecCombopVersionConfigVo 组合工具config
     */
    void updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(AutoexecCombopVersionConfigVo autoexecCombopVersionConfigVo);

    /**
     * 保存组合工具权限
     * @param autoexecCombopVo
     */
    void saveAuthority(AutoexecCombopVo autoexecCombopVo);

    /**
     * 对密码类型参数的值加密处理
     * @param config
     */
    void passwordParamEncrypt(AutoexecCombopVersionConfigVo config);

    AutoexecCombopVo getAutoexecCombopById(Long id);

    void saveAutoexecCombop(AutoexecCombopVo autoexecCombopVo);

    void saveAutoexecCombopVersion(AutoexecCombopVersionVo autoexecCombopVersionVo);
}
