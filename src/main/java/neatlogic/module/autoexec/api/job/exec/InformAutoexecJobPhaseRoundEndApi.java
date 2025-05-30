/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobGroupVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobGroupNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InformAutoexecJobPhaseRoundEndApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "激活作业下一阶段round";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "term.autoexec.phase", isRequired = true),
            @Param(name = "execId", type = ApiParamType.LONG, desc = "nmaaje.updateautoexecjobphasestatusapi.input.param.desc", isRequired = true),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "runnerId"),
            @Param(name = "roundNo", type = ApiParamType.INTEGER, desc = "round no", isRequired = true),
            @Param(name = "groupNo", type = ApiParamType.INTEGER, desc = "group no", isRequired = true),
            @Param(name = "seqNo", type = ApiParamType.INTEGER, desc = "nmaaje.informautoexecjobphaseroundendapi.input.param.desc.seqno"),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.passthroughenv", isRequired = true),
            @Param(name = "time", type = ApiParamType.DOUBLE, desc = "common.time")
    })
    @Output({
    })
    @Description(desc = "激活作业下一个round")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phase");
        Integer roundNo = jsonObj.getInteger("roundNo");
        Integer groupSort = jsonObj.getInteger("groupNo");
        Integer seqNo = jsonObj.getInteger("seqNo");
        boolean isJobPhaseRoundNodeAllCompleted = false;
        //Long runnerId = jsonObj.getLong("runnerId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobGroupVo groupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobId, groupSort);
        if (groupVo == null) {
            throw new AutoexecJobGroupNotFoundException(jobId, groupSort);
        }
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phase);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + phase);
        }
        autoexecJobActionService.initExecuteUserContext(jobVo);

        if (seqNo == null) {
            isJobPhaseRoundNodeAllCompleted = isJobPhaseRoundNodeAllCompleted(groupVo, jobPhaseVo, roundNo);
        } else {
            isJobPhaseRoundNodeAllCompleted = isJobPhaseSeqNoRoundNodeAllCompleted(seqNo, jobVo, jobPhaseVo);
        }

        //判断该phase这个round所属节点是否都跑完了
        if (Objects.equals(jobPhaseVo.getExecMode(), ExecMode.RUNNER.getValue()) || isJobPhaseRoundNodeAllCompleted) {
            //发起inform
            IAutoexecJobActionHandler jobActionHandler = AutoexecJobActionHandlerFactory.getAction(JobAction.INFORM_PHASE_ROUND.getValue());
            jobVo.setAction(JobAction.INFORM_PHASE_ROUND.getValue());
            jobVo.setActionParam(jsonObj);
            jobVo.setExecutePhase(jobPhaseVo);
            jobActionHandler.doService(jobVo);
            //System.out.println("roundNo:"+jsonObj.getInteger("roundNo")+" runnerId:"+jsonObj.getString(("runnerId")) +" phase:"+ phase + " run");
        }
        //else{
        //System.out.println(jsonObj.getString(("runnerId")) +" "+ roundNo+" "+ "wait");
        //}
        return null;
    }

    /**
     * 当前蓝绿批次（seqNo）的节点是否都执行完了
     *
     * @param seqNo   蓝绿批次
     * @param jobVo   作业
     * @param phaseVo 阶段
     */
    private boolean isJobPhaseSeqNoRoundNodeAllCompleted(Integer seqNo, AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo) {
        if (Objects.equals(ExecMode.SQL.getValue(), phaseVo.getExecMode())) {
            List<AutoexecJobPhaseNodeVo> nodeVos = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobVo.getId(), phaseVo.getId());
            if (CollectionUtils.isEmpty(nodeVos)) {
                throw new AutoexecJobPhaseNodeNotFoundException(phaseVo.getId().toString(), ExecMode.SQL.getValue());
            }
            return nodeVos.stream().allMatch(o -> Arrays.asList(JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue()).contains(o.getStatus()));
        } else {
            AutoexecJobPhaseNodeVo nodeParamVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), phaseVo.getName(), 0);
            nodeParamVo.setStatusBlackList(Arrays.asList(JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue()));
            List<AutoexecJobPhaseNodeVo> notCompleteNode = autoexecJobMapper.getJobPhaseNodeIdListByNodeVoAndSeqNo(nodeParamVo, seqNo);
            return CollectionUtils.isEmpty(notCompleteNode);
        }
    }


    /**
     * @param jobGroup 作业组
     * @param phaseVo  阶段
     * @param roundNo  round号
     */
    private boolean isJobPhaseRoundNodeAllCompleted(AutoexecJobGroupVo jobGroup, AutoexecJobPhaseVo phaseVo, Integer roundNo) {
        List<Integer> roundCountList = new ArrayList<>();
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        Integer roundCount = jobVo.getRoundCount();
        if (jobGroup.getRoundCount() != null) {
            roundCount = phaseVo.getRoundCount();
        }
        if (phaseVo.getRoundCount() != null) {
            roundCount = phaseVo.getRoundCount();
        }
        AutoexecJobPhaseNodeVo nodeParamVo = new AutoexecJobPhaseNodeVo(jobVo.getId(), phaseVo.getName(), 0);
        int totalNodeCount = autoexecJobMapper.searchJobPhaseNodeCount(nodeParamVo);
        if (roundCount == null || roundCount < 0) {
            roundCount = 2;
        }
        //全部串行
        if (roundCount == 0) {
            roundCount = totalNodeCount;
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
            } else if (i <= remainder) {
                roundCountList.add(parallelCount + 1);
            } else {
                roundCountList.add(parallelCount);
            }
            if (roundNo > i) {
                startNum += roundCountList.get(i - 1);
            }
        }
        //如果超过最大round，表示该group所有round都跑完了
        if (roundCountList.size() < roundNo) {
            return true;
        }
        //设置分页，查询该phase round
        nodeParamVo.setPageSize(roundCountList.get(roundNo - 1));
        return autoexecJobMapper.getJobPhaseNodeIdListByNodeVoAndStartNum(nodeParamVo, startNum).stream().allMatch(o -> Arrays.asList(JobNodeStatus.IGNORED.getValue(),JobNodeStatus.SUCCEED.getValue()).contains(o.getStatus()));
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/inform/round/end";
    }
}
