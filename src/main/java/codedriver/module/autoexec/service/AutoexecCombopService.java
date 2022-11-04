/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;

import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/15 16:13
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
     * @return
     */
    default boolean checkOperableButton(AutoexecCombopVo autoexecCombopVo, CombopAuthorityAction action) {
        return checkOperableButton(autoexecCombopVo, action, UserContext.get().getUserUuid(true));
    }

    /**
     * 检查当前用户是否有当前组合工具的某项权限
     *
     * @param autoexecCombopVo 组合工具
     * @param action           权限
     * @param user             指定校验权限的用户
     * @return
     */
    boolean checkOperableButton(AutoexecCombopVo autoexecCombopVo, CombopAuthorityAction action, String user);

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
     * 保存组合工具配置信息
     *
     * @param autoexecCombopVo
     * @param isCopy
     */
    void saveAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo, boolean isCopy);

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
     * 获取组合工具信息
     *
     * @param id
     * @return
     */
    AutoexecCombopVo getAutoexecCombopById(Long id);

    /**
     * 根据protocolId补充protocol字段和protocolPort字段值
     * @param autoexecCombopConfigVo 组合工具config
     */
    void updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(AutoexecCombopConfigVo autoexecCombopConfigVo);
}
