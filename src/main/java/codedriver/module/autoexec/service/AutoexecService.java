/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;

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

    void updateAutoexecCombopConfig(AutoexecCombopConfigVo config);

    /**
     * 根据id和类型获取自定义工具或工具信息
     * @param id 自定义工具id或工具id
     * @param type 类型
     * @return
     */
    AutoexecOperationBaseVo getAutoexecOperationBaseVoByIdAndType(Long id, String type);

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

}
