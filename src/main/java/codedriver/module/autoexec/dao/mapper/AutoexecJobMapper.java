/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.job.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AutoexecJobMapper {
    List<Long> searchAutoexecJobId(AutoexecJobVo jobVo);

    List<AutoexecJobVo> searchAutoexecJob(List<Long> jobIdList);

    AutoexecJobVo getAutoexecJobInfo(Long jobId);

    List<AutoexecJobPhaseNodeVo> searchAutoexecJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    int searchAutoexecJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    int searchAutoexecJobCount(AutoexecJobVo jobVo);

    List<AutoexecJobPhaseVo> getJobPhaseListByJobId(Long jobId);

    List<AutoexecJobPhaseNodeStatusCountVo> getJobPhaseNodeStatusCount(Long jobId);

    AutoexecJobPhaseVo getJobPhaseLockByPhaseId(Long jobPhaseId);

    int checkIsAutoexecJobUser(@Param("jobId")Long jobId,@Param("userList") List<String> userList);

    int insertAutoexecJob(AutoexecJobVo jobVo);

    int insertAutoexecJobPhase(AutoexecJobPhaseVo jobVo);

    int insertAutoexecJobPhaseNode(AutoexecJobPhaseNodeVo jobVo);

    int insertAutoexecJobPhaseOperation(AutoexecJobPhaseOperationVo operationVo);

    int insertAutoexecJobParamContent(AutoexecJobParamContentVo contentVo);

    int updateJobPhaseStatus(AutoexecJobPhaseVo autoexecJobPhaseVo);
}
