/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.job.group.policy;

import neatlogic.framework.autoexec.constvalue.AutoexecJobGroupPolicy;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseRunnerNotFoundException;
import neatlogic.framework.autoexec.job.group.policy.core.AutoexecJobGroupPolicyHandlerBase;
import neatlogic.framework.dto.runner.RunnerMapVo;
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
            throw new AutoexecJobPhaseRunnerNotFoundException(jobVo.getPhaseNameList().stream().map(Object::toString).collect(Collectors.joining("','")));
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
