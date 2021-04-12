/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.*;

import java.util.List;

public interface AutoexecJobMapper {
    List<AutoexecJobVo> searchAutoexecJob(AutoexecJobVo jobVo);

    int insertAutoexecJob(AutoexecJobVo jobVo);

    int insertAutoexecJobPhase(AutoexecJobPhaseVo jobVo);

    int insertAutoexecJobPhaseNode(AutoexecJobPhaseNodeVo jobVo);

    int insertAutoexecJobPhaseOperation(AutoexecJobPhaseOperationVo operationVo);

    int insertAutoexecJobParamContent(AutoexecJobParamContentVo contentVo);

}
