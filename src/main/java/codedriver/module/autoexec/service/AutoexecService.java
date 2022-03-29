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

    void mergeConfig(AutoexecParamVo autoexecParamVo);

    List<AutoexecJobVo> getJobList(AutoexecJobVo jobVo);

    void updateAutoexecCombopConfig(AutoexecCombopConfigVo config);

    /**
     * 根据关联的operationVoList获取工具参数并与数据库存储的旧参数oldOperationParamList做去重处理
     *
     * @param paramAutoexecOperationVoList
     * @param oldOperationParamList
     * @return
     */
    List<AutoexecParamVo> getProfileConfig(List<AutoexecOperationVo> paramAutoexecOperationVoList, List<AutoexecParamVo> oldOperationParamList);

    /**
     * 根据关联的operationVoList获取工具参数并与数据库存储的旧参数oldOperationParamList做去重处理
     *
     * @param paramAutoexecOperationVoList
     * @return
     */
    List<AutoexecParamVo> getProfileConfig(List<AutoexecOperationVo> paramAutoexecOperationVoList);
}
