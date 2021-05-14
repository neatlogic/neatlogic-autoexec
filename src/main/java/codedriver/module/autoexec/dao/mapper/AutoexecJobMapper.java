/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AutoexecJobMapper {
    //job
    List<Long> searchJobId(AutoexecJobVo jobVo);

    List<AutoexecJobVo> searchJob(List<Long> jobIdList);

    AutoexecJobVo getJobInfo(Long jobId);

    AutoexecJobVo getJobDetailByJobIdAndPhaseName(@Param("jobId")Long jobId,@Param("phaseName") String phaseName);

    Integer searchJobCount(AutoexecJobVo jobVo);

    List<AutoexecCombopVo> searchJobWithCombopView(AutoexecJobVo jobVo);

    Integer checkIsJobUser(@Param("jobId") Long jobId, @Param("userList") List<String> userList);

    AutoexecJobVo getJobLockByJobId(Long jobId);

    AutoexecJobParamContentVo getJobParamContentLock(String hash);

    int checkIsJobParamReference(@Param("jobId") Long jobId, @Param("hash") String hash);

    void updateJobStatus(AutoexecJobVo jobVo);

    //jobNodes
    int getJobPhaseNodeCountByJobId(AutoexecJobPhaseNodeVo nodeParamVo);

    List<AutoexecJobPhaseNodeVo> searchJobNodeByJobId(AutoexecJobPhaseNodeVo nodeParamVo);

    //jobPhase
    List<AutoexecJobPhaseVo> getJobPhaseListByJobId(Long jobId);

    AutoexecJobPhaseVo getJobPhaseLockByPhaseId(Long jobPhaseId);

    AutoexecJobPhaseVo getJobPhaseLockByJobIdAndPhaseName(@Param("jobId") Long jobId,@Param("jobPhaseName") String jobPhaseName);

    AutoexecJobPhaseVo getFirstJobPhase(Long jobId);

    //jobPhaseNode
    List<AutoexecJobPhaseNodeVo> searchJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    int searchJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<AutoexecJobPhaseNodeStatusCountVo> getJobPhaseNodeStatusCount(Long jobId);

    //jobPhaseOperation
    List<AutoexecJobPhaseOperationVo> getJobPhaseOperationByJobId(Long jobId);

    int checkIsJobPhaseOperationParamReference(@Param("jobId") Long jobId, @Param("hash") String hash);

    //jobParamContent



    Integer insertJob(AutoexecJobVo jobVo);

    Integer insertJobPhase(AutoexecJobPhaseVo jobVo);

    Integer insertJobPhaseNode(AutoexecJobPhaseNodeVo jobVo);

    Integer insertJobPhaseOperation(AutoexecJobPhaseOperationVo operationVo);

    Integer insertJobParamContent(AutoexecJobParamContentVo contentVo);

    Integer updateJobPhaseStatus(AutoexecJobPhaseVo autoexecJobPhaseVo);


    void deleteJobParamContentByHash(String paramHash);

    void deleteJobUserByJobId(Long jobId);

    void deleteJobPhaseOperationByJobId(Long jobId);

    void deleteJobPhaseNodeByJobId(Long jobId);

    void deleteJobPhaseByJobId(Long jobId);

    void deleteJobByJobId(Long jobId);

}
