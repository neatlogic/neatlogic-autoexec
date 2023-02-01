/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.dto.AutoexecOperationBaseVo;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;

import java.util.List;

public interface AutoexecService {

    /**
     * 校验参数
     *
     * @param paramList 参数列表
     */
    void validateParamList(List<? extends AutoexecParamVo> paramList);

    /**
     * 校验自由参数
     *
     * @param argument 自由参数
     */
    void validateArgument(AutoexecParamVo argument);

    /**
     * 检验运行参数列表
     * @param runtimeParamList 作业参数列表
     */
    void validateRuntimeParamList(List<? extends AutoexecParamVo> runtimeParamList);

    void mergeConfig(AutoexecParamVo autoexecParamVo);

    /**
     * 补充AutoexecCombopConfigVo对象中的场景名称、预置参数集名称、操作对应的工具信息
     * @param config config对象
     */
    void updateAutoexecCombopConfig(AutoexecCombopConfigVo config);

    /**
     * 补充AutoexecCombopVersionConfigVo对象中的预置参数集名称、操作对应的工具信息
     * @param config config对象
     */
    void updateAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo config);

    /**
     * 补充阶段操作中的自定义工具或工具信息
     * @param phaseName 阶段名称
     * @param autoexecCombopPhaseOperationVo 阶段操作信息
     * @param throwException 是否抛异常
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
     * @param profileName 预置参数名称
     * @param operatioinId 预置参数ID
     * @param operationType 类型（工具或自定义工具）
     * @Return profileId
     */
    Long saveProfileOperation(String profileName, Long operatioinId, String operationType);

    /**
     * 检验文本类型参数值
     * @param autoexecParamVo 参数信息
     * @param value 参数值
     */
    boolean validateTextTypeParamValue(AutoexecParamVo autoexecParamVo, Object value);

}
