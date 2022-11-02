/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;

import java.util.List;

public interface AutoexecService {

    /**
     * 校验参数
     *
     * @param paramList
     */
    void validateParamList(List<? extends AutoexecParamVo> paramList);

    /**
     * 校验自由参数
     *
     * @param argument
     */
    void validateArgument(AutoexecParamVo argument);

    /**
     * 检验运行参数列表
     * @param runtimeParamList
     */
    void validateRuntimeParamList(List<? extends AutoexecParamVo> runtimeParamList);

    void mergeConfig(AutoexecParamVo autoexecParamVo);

    /**
     * 补充AutoexecCombopConfigVo对象中的场景名称、预置参数集名称、操作对应的工具信息
     * @param config config对象
     */
    void updateAutoexecCombopConfig(AutoexecCombopConfigVo config);

    /**
     * 补充阶段操作中的自定义工具或工具信息
     * @param autoexecCombopPhaseOperationVo
     * @return
     */
    AutoexecOperationBaseVo getAutoexecOperationBaseVoByIdAndType(String phaseName, AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo, boolean throwException);

    /**
     * 根据关operationVoList获取工具参数并做去重处理
     *
     * @param paramAutoexecOperationVoList 工具list
     * @return
     */
    List<AutoexecParamVo> getAutoexecOperationParamVoList(List<AutoexecOperationVo> paramAutoexecOperationVoList);

    /**
     * 根据scriptIdList和toolIdList获取对应的operationVoList
     *
     * @param scriptIdList 脚本idList
     * @param toolIdList   工具idList
     * @return
     */
    List<AutoexecOperationVo> getAutoexecOperationByScriptIdAndToolIdList(List<Long> scriptIdList, List<Long> toolIdList);

    /**
     * 保存profile与工具或自定义工具的关联
     * 若不存在名为{profileName}的profile，则先创建
     *
     * @param profileName
     * @param operatioinId
     * @param operationType 类型（工具或自定义工具）
     * @Return profileId
     */
    Long saveProfileOperation(String profileName, Long operatioinId, String operationType);

    /**
     * 检验文本类型参数值
     * @param autoexecParamVo
     */
    boolean validateTextTypeParamValue(AutoexecParamVo autoexecParamVo);
    /**
     * 检验文本类型参数默认值
     * @param autoexecParamVo
     */
    boolean validateTextTypeParamDefaultValue(AutoexecParamVo autoexecParamVo);

}
