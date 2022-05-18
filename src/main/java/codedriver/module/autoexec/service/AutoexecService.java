/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;

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

    void mergeConfig(AutoexecParamVo autoexecParamVo);

    List<AutoexecJobVo> getJobList(AutoexecJobVo jobVo);

    void updateAutoexecCombopConfig(AutoexecCombopConfigVo config);

    /**
     * 根据关联的operationVoList获取工具参数并与数据库存储的旧参数oldOperationParamList做去重处理
     *
     * @param paramAutoexecOperationVoList 工具list
     * @param oldOperationParamList        旧的参数list
     * @return
     */
    List<AutoexecParamVo> getAutoexecOperationParamVoList(List<AutoexecOperationVo> paramAutoexecOperationVoList, List<AutoexecParamVo> oldOperationParamList);

    /**
     * 根据关联的operationVoList获取工具参数并与数据库存储的旧参数oldOperationParamList做去重处理
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
     * 根据作业id和剧本名称重置sql文件状态
     *
     * @param jobId            作业id
     * @param jobPhaseNameList 作业剧本列表
     */
    void resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameList(Long jobId, List<String> jobPhaseNameList);

    /**
     * 根据作业id和剧本名称和sql文件列表重置sql文件转台
     * @param jobId 作业id
     * @param jobPhaseName 作业阶段名称
     * @param sqlFileList sql文件列表
     */
    void resetAutoexecJobSqlStatusByJobIdAndJobPhaseNameAndSqlFileList(Long jobId, String jobPhaseName, List<String> sqlFileList);
}
