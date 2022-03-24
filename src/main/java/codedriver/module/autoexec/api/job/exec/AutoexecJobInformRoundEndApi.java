/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
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
public class AutoexecJobInformRoundEndApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    UserMapper userMapper;

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
            @Param(name = "roundNo", type = ApiParamType.STRING, desc = "round 号", isRequired = true),
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
        Long runnerId;
        Integer groupSort;
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");

        if (MapUtils.isEmpty(passThroughEnv)) {
            throw new ParamIrregularException("passThroughEnv");
        }
        if (!passThroughEnv.containsKey("runnerId")) {
            throw new ParamIrregularException("runnerId");
        } else {
            runnerId = passThroughEnv.getLong("runnerId");
        }

        if (!passThroughEnv.containsKey("groupSort")) {
            throw new ParamIrregularException("groupSort");
        } else {
            groupSort = passThroughEnv.getInteger("groupSort");
        }
        //更新phase对应runner的"是否fireNext"标识为1
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phase);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + phase);
        }
        //autoexecJobMapper.updateJobPhaseRunnerFireNextByPhaseIdAndRunnerId(jobPhaseVo.getId(), 1, runnerId);
        //判断该phase这个round所属节点是否都跑完了


        return null;
    }


    private List<Integer> getRoundCountList(AutoexecJobPhaseVo phaseVo,Integer roundNo) {
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
            } else if (roundNo <= remainder ) {
                roundCountList.add(parallelCount + 1);
            }
            if(roundNo > i) {
                startNum += roundCountList.get(i - 1);
            }
        }
        //设置分页，查询该phase round
        nodeParamVo.setPageSize(roundCountList.get(roundNo-1));
        nodeParamVo.setStatus(JobNodeStatus.PENDING.getValue());
        if(autoexecJobMapper.getJobPhaseNodeByNodeVoAndStartNumCount(nodeParamVo,startNum) > 0){}



        return roundCountList;
    }

    @Override
    public String getToken() {
        return "autoexec/job/next/phase/fire";
    }
}
