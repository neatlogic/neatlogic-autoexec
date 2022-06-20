/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.group.policy;

import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.group.policy.core.AutoexecJobGroupPolicyHandlerBase;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.autoexec.constvalue.AutoexecJobGroupPolicy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Service
public class AutoexecJobOneShotHandler extends AutoexecJobGroupPolicyHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return AutoexecJobGroupPolicy.ONESHOT.getName();
    }

    @Override
    public void getExecutePhaseList(AutoexecJobVo jobVo) {
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobVo.getId(),jobVo.getExecuteJobGroupVo().getSort());
        jobVo.setExecuteJobPhaseList(jobPhaseVoList);
    }

    @Override
    public List<RunnerMapVo> getExecuteRunnerList(AutoexecJobVo jobVo) {
        List<RunnerMapVo> runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), jobVo.getExecuteJobPhaseList().stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(runnerVos)) {
            throw new AutoexecJobRunnerNotFoundException(jobVo.getPhaseNameList());
        }
        return runnerVos.stream().filter(o-> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>( Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));
    }

    @Override
    public void updateExecutePhaseListStatus(AutoexecJobVo jobVo) {
        for (AutoexecJobPhaseVo jobPhase : jobVo.getExecuteJobPhaseList()) {
            jobPhase.setStatus(JobPhaseStatus.WAITING.getValue());
            autoexecJobMapper.updateJobPhaseStatus(jobPhase);
        }
    }
}
