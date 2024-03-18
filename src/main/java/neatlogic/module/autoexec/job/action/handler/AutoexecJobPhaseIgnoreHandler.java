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

package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseRunnerNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/10/24 12:18
 **/
@Service
public class AutoexecJobPhaseIgnoreHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.IGNORE_PHASE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        //目前仅支持忽略 runner阶段
        if (Objects.equals(jobPhaseVo.getExecMode(), ExecMode.RUNNER.getValue())) {
            jobPhaseVo.setStatus(JobPhaseStatus.IGNORED.getValue());
            autoexecJobMapper.updateJobPhaseStatus(jobPhaseVo);
            List<RunnerMapVo> runnerMapVos = autoexecJobMapper.getJobPhaseNodeRunnerListByJobPhaseId(jobPhaseVo.getId());
            if(CollectionUtils.isEmpty(runnerMapVos)){
                throw new AutoexecJobPhaseRunnerNotFoundException(jobPhaseVo.getJobId(),jobPhaseVo.getName(),jobPhaseVo.getId());
            }
            jobVo.setExecuteJobNodeVoList(Collections.singletonList(new AutoexecJobPhaseNodeVo("host",0,0L)));
            autoexecJobMapper.updateJobPhaseRunnerStatus(Collections.singletonList(jobPhaseVo.getId()),runnerMapVos.get(0).getRunnerMapId(),JobPhaseStatus.IGNORED.getValue());
            autoexecJobMapper.updateJobPhaseNodeStatusByJobPhaseIdAndIsDelete(jobPhaseVo.getId(), JobPhaseStatus.IGNORED.getValue(), 0);
            autoexecJobService.updateJobNodeStatus(runnerMapVos, jobVo, JobNodeStatus.IGNORED.getValue());
        }
        return null;
    }
}
