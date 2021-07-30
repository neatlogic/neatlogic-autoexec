/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.AutoexecRunnerVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AutoexecJobMapper {
    //job
    List<Long> searchJobId(AutoexecJobVo jobVo);

    List<AutoexecJobVo> searchJob(List<Long> jobIdList);

    AutoexecJobVo getJobInfo(Long jobId);

    AutoexecJobVo getJobDetailByJobIdAndPhaseName(@Param("jobId") Long jobId, @Param("phaseName") String phaseName);

    Integer searchJobCount(AutoexecJobVo jobVo);

    List<AutoexecCombopVo> searchJobWithCombopView(AutoexecJobVo jobVo);

    Integer checkIsJobUser(@Param("jobId") Long jobId, @Param("userList") List<String> userList);

    AutoexecJobVo getJobLockByJobId(Long jobId);

    AutoexecJobParamContentVo getJobParamContentLock(String hash);

    AutoexecJobParamContentVo getJobParamContent(String hash);

    int checkIsJobParamReference(@Param("jobId") Long jobId, @Param("hash") String hash);

    void updateJobStatus(AutoexecJobVo jobVo);

    AutoexecJobVo getJobLockByOperationId(Long operationId);

    //jobNodes
    int getJobPhaseNodeCountByJobId(AutoexecJobPhaseNodeVo nodeParamVo);

    List<AutoexecJobPhaseNodeVo> searchJobNodeByJobId(AutoexecJobPhaseNodeVo nodeParamVo);

    //jobPhase
    List<AutoexecJobPhaseVo> getJobPhaseListByJobId(Long jobId);

    List<AutoexecJobPhaseVo> getJobPhaseListByJobIdAndPhaseStatus(@Param("jobId") Long jobId, @Param("statusList") List<String> statusList);

    AutoexecJobPhaseVo getJobPhaseLockByPhaseId(Long jobPhaseId);

    AutoexecJobPhaseVo getJobPhaseByPhaseId(Long jobPhaseId);

    AutoexecJobPhaseVo getJobPhaseLockByJobIdAndPhaseName(@Param("jobId") Long jobId, @Param("jobPhaseName") String jobPhaseName);

    AutoexecJobPhaseVo getJobPhaseByJobIdAndPhaseId(@Param("jobId") Long jobId, @Param("jobPhaseId") Long jobPhaseId);

    AutoexecJobPhaseVo getFirstJobPhase(Long jobId);

    AutoexecJobPhaseVo getJobPhaseByJobIdAndPhaseName(@Param("jobId") Long jobId, @Param("jobPhaseName") String jobPhaseName);

    Integer checkIsAllActivePhaseIsDone(@Param("jobId") Long jobId, @Param("sort") Integer sort);

    List<AutoexecJobPhaseVo> getJobPhaseListByJobIdAndSort(@Param("jobId") Long jobId, @Param("sort") Integer sort);

    Integer checkIsHasActivePhaseFailed(Long jobId);

    int getJobPhaseRunnerByNotStatusCount(@Param("jobPhaseIdList") List<Long> jobPhaseIdList, @Param("status") String status);

    int getJobPhaseRunnerByStatusCount(@Param("jobPhaseIdList") List<Long> jobPhaseIdList, @Param("status") String status);

    List<AutoexecJobPhaseVo> getJobPhaseRunnerCountByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") String status);

    AutoexecJobPhaseVo getJobPhaseByJobIdAndPhaseStatus(@Param("jobId") Long id, @Param("status") String status);

    AutoexecJobPhaseVo getJobCurrentPhase(Long jobId);

    //jobPhaseNode
    List<AutoexecJobPhaseNodeVo> searchJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    int searchJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<AutoexecJobPhaseNodeStatusCountVo> getJobPhaseNodeStatusCount(Long jobId);

    int checkIsJobPhaseNodeExist(AutoexecJobPhaseNodeVo nodeVo);

    int updateJobPhaseNodeStatus(AutoexecJobPhaseNodeVo nodeVo);

    AutoexecJobPhaseNodeVo getJobPhaseNodeInfoByJobNodeId(@Param("nodeId") Long nodeId);

    AutoexecJobPhaseNodeVo getJobPhaseRunnerNodeByJobIdAndPhaseId(@Param("jobId") Long jobId, @Param("phaseId") Long phaseId);

    List<AutoexecJobPhaseNodeVo> getJobPhaseNodeListByJobIdAndPhaseNameAndExceptStatusAndRunnerId(@Param("jobId") Long jobId, @Param("phaseName") String phaseName, @Param("exceptStatus") List<String> exceptStatus, @Param("runnerId") Integer runnerId);

    List<AutoexecJobPhaseNodeVo> getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatus(@Param("jobId") Long jobId, @Param("phaseId") Long phaseId, @Param("exceptStatus") List<String> exceptStatus);

    List<AutoexecJobPhaseNodeVo> getJobPhaseNodeListByJobIdAndPhaseId(@Param("jobId") Long jobId, @Param("phaseId") Long phaseId);

    List<AutoexecJobPhaseNodeVo> getJobPhaseNodeListByNodeIdList(@Param("nodeIdList") List<Long> nodeIdList);

    List<AutoexecJobPhaseNodeVo> getJobPhaseNodeListByJobIdAndNodeStatusList(@Param("jobId") Long jobId, @Param("statusList") List<String> statusList);

    int checkIsHasRunningNode(Long id);

    int updateJobPhaseNodeListStatus(@Param("nodeIdList") List<Long> jobPhaseNodeIdList, @Param("status") String status);

    //jobPhaseOperation
    List<AutoexecJobPhaseOperationVo> getJobPhaseOperationByJobId(Long jobId);

    List<AutoexecJobPhaseOperationVo> getJobPhaseOperationByJobIdAndPhaseId(@Param("jobId") Long jobId, @Param("phaseId") Long phaseId);

    AutoexecJobPhaseOperationVo getJobPhaseOperationByJobIdAndPhaseIdAndOperationId(@Param("jobId") Long jobId, @Param("jobPhaseId") Long jobPhaseId, @Param("jobPhaseOperationId") Long operationId);

    AutoexecJobPhaseOperationVo getJobPhaseOperationByOperationId(@Param("jobPhaseOperationId") Long operationId);

    int checkIsJobPhaseOperationParamReference(@Param("jobId") Long jobId, @Param("hash") String hash);

    //jobParamContent
    Integer getNextJobPhaseSortByJobId(@Param("jobId") Long jobId, @Param("sort") Integer sort);

    List<AutoexecJobParamContentVo> getJobParamContentList(@Param("hashList") List<String> hashList);

    //runner
    List<AutoexecRunnerVo> getJobRunnerListByJobId(Long jobId);

    AutoexecRunnerVo getJobRunnerById(Integer runnerId);

    List<AutoexecRunnerVo> getJobPhaseRunnerByJobIdAndPhaseIdList(@Param("jobId") Long jobId, @Param("jobPhaseIdList") List<Long> jobPhaseId);

    Integer insertJobPhaseNodeRunner(@Param("nodeId") Long nodeId, @Param("runnerMapId") Integer runnerMapId);

    Integer insertJobPhaseRunner(@Param("jobId") Long jobId, @Param("jobPhaseId") Long jobPhaseId, @Param("runnerMapId") Integer runnerMapId);

    Integer insertJob(AutoexecJobVo jobVo);

    Integer insertJobPhase(AutoexecJobPhaseVo jobVo);

    Integer insertJobNode(AutoexecJobNodeVo jobNodeVo);

    Integer insertJobPhaseNode(AutoexecJobPhaseNodeVo jobVo);

    Integer insertJobPhaseOperation(AutoexecJobPhaseOperationVo operationVo);

    Integer insertJobParamContent(AutoexecJobParamContentVo contentVo);

    Integer updateJobPhaseStatus(AutoexecJobPhaseVo autoexecJobPhaseVo);

    Integer updateJobPhaseFailedNodeStatusByJobId(@Param("jobId") Long id, @Param("status") String value);

    Integer updateJobPhaseStatusBatch(@Param("phaseIdList") List<Long> phaseIdList, @Param("status") String phaseStatus, @Param("errorMsg") String errorMsg);

    void updateJobPhaseStatusByJobId(@Param("jobId") Long id, @Param("status") String value);

    Integer updateJobPhaseRunnerStatus(@Param("jobPhaseIdList") List<Long> jobPhaseIdList, @Param("runnerId") Integer runnerId, @Param("status") String status);

    Integer updateBatchJobPhaseRunnerStatus(@Param("jobPhaseId") Long jobPhaseId, @Param("status") String status);

    Integer updateJobPhaseStatusByPhaseIdList(@Param("phaseIdList") List<Long> phaseIdList, @Param("status") String status);

    Integer updateJobPhaseNode(AutoexecJobPhaseNodeVo nodeVo);

    void deleteJobParamContentByHash(String paramHash);

    void deleteJobPhaseOperationByJobId(Long jobId);

    void deleteJobPhaseNodeByJobId(Long jobId);

    void deleteJobPhaseByJobId(Long jobId);

    void deleteJobByJobId(Long jobId);

    void deleteJobPhaseNodeByJobPhaseIdList(@Param("jobPhaseIdList") List<Long> jobPhaseIdList);

    void deleteJobPhaseRunnerByJobId(Long jobId);

    void deleteJobPhaseNodeRunnerByJobId(Long jobId);

}
