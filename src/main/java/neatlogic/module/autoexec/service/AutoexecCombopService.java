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
     * @param autoexecCombopVo      组合工具信息
     * @param autoexecCombopPhaseVo 阶段信息
     */
    void needExecuteConfig(AutoexecCombopVo autoexecCombopVo, AutoexecCombopPhaseVo autoexecCombopPhaseVo);

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
     * 保存组合工具配置信息
     *
     * @param autoexecCombopVo
     * @param isCopy
     */
    @Deprecated
    void saveAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo, boolean isCopy);

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
     * 预先准备组合工具版本配置信息
     *
     * @param autoexecCombopVersionConfigVo
     * @param isCopy
     */
    void prepareAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo autoexecCombopVersionConfigVo, boolean isCopy);

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
     * @param autoexecCombopConfigVo 组合工具config
     */
    void updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(AutoexecCombopConfigVo autoexecCombopConfigVo);

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
}
