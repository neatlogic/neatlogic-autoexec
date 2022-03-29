/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotCompletedException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobPhaseInformRoundEndApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "激活作业下一阶段round";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "runnerId"),
            @Param(name = "roundNo", type = ApiParamType.INTEGER, desc = "round号", isRequired = true),
            @Param(name = "groupNo", type = ApiParamType.INTEGER, desc = "组号", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数", isRequired = true),
            @Param(name = "time", type = ApiParamType.DOUBLE, desc = "回调时间")
    })
    @Output({
    })
    @Description(desc = "激活作业下一阶段round")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("lastPhase");
        Integer roundNo = jsonObj.getInteger("roundNo");
        Integer groupSort = jsonObj.getInteger("groupNo");
        //Long runnerId = jsonObj.getLong("runnerId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phase);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + phase);
        }
        //判断该phase这个round所属节点是否都跑完了
        isJobPhaseRoundNodeAllCompleted(groupSort, jobPhaseVo, roundNo);
        //发起inform
        IAutoexecJobActionHandler jobActionHandler = AutoexecJobActionHandlerFactory.getAction(JobAction.INFORM_PHASE_ROUND.getValue());
        jobVo.setAction(JobAction.INFORM_PHASE_ROUND.getValue());
        jobVo.setActionParam(jsonObj);
        jobVo.setPhaseList(Collections.singletonList(jobPhaseVo));
        jobActionHandler.doService(jobVo);
        return null;
    }

    /**
     *
     * @param jobGroupSort 作业组序号
     * @param phaseVo 阶段
     * @param roundNo round号
     */
    private void isJobPhaseRoundNodeAllCompleted(Integer jobGroupSort, AutoexecJobPhaseVo phaseVo, Integer roundNo) {
        List<Integer> roundCountList = new ArrayList<>();
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        Integer roundCount = jobVo.getThreadCount();
        AutoexecJobPhaseNodeVo nodeParamVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), phaseVo.getName(), 0);
        nodeParamVo.setStatusBlackList(Collections.singletonList(JobNodeStatus.IGNORED.getValue()));
        int totalNodeCount = autoexecJobMapper.searchJobPhaseNodeCount(nodeParamVo);
        if (roundCount == null || roundCount <= 0) {
            roundCount = 2;
        }
        int parallelCount = totalNodeCount / roundCount;
        int remainder = totalNodeCount % roundCount;
        if (parallelCount == 0) {
            roundCount = totalNodeCount;
        }
        //得出每个round的count 列表
        int startNum = 0;
        for (int i = 1; i <= roundCount; i++) {
            if (parallelCount == 0) {
                roundCountList.add(1);
            } else if (roundNo <= remainder) {
                roundCountList.add(parallelCount + 1);
            }
            if (roundNo > i) {
                startNum += roundCountList.get(i - 1);
            }
        }
        //设置分页，查询该phase round
        nodeParamVo.setPageSize(roundCountList.get(roundNo - 1));
        nodeParamVo.setStatusList(Collections.singletonList(JobNodeStatus.PENDING.getValue()));
        List<Long> notCompletedNodeIdList = autoexecJobMapper.getJobPhaseNodeByNodeVoAndStartNumCount(nodeParamVo, startNum);
        if (CollectionUtils.isEmpty(notCompletedNodeIdList)) {
            throw new AutoexecJobPhaseNodeNotCompletedException(notCompletedNodeIdList);
        }
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/inform/round/end";
    }
}
