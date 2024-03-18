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
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobStatusApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "nmaaje.updateautoexecjobstatusapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "common.status", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.passthroughenv")
    })
    @Output({
    })
    @Description(desc = "nmaaje.updateautoexecjobstatusapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String status = jsonObj.getString("status");
        String statusIng = null;
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //更新执行用户上下文
        autoexecJobActionService.initExecuteUserContext(jobVo);

        if (Objects.equals(status, JobStatus.ABORTED.getValue())) {
            statusIng = JobStatus.ABORTING.getValue();
        } else if (Objects.equals(status, JobStatus.PAUSED.getValue())) {
            statusIng = JobStatus.PAUSING.getValue();
        }

        if (StringUtils.isNotBlank(statusIng)) {
            //if(Objects.equals(statusIng, jobVo.getStatus())) {
            jobVo.setStatus(status);
            if (!jsonObj.containsKey("passThroughEnv")) {
                throw new ParamIrregularException("passThroughEnv");
            }
            JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
            if (!passThroughEnv.containsKey("runnerId")) {
                throw new ParamIrregularException("runnerId");
            }
            Long runnerId = passThroughEnv.getLong("runnerId");
            jobVo.setPassThroughEnv(passThroughEnv);
            //update job phase runner
            autoexecJobMapper.updateJobPhaseRunnerStatusByJobIdAndRunnerIdAndStatus(jobId, runnerId, status);
            //如果该job runner 没有一个aborting|pausing phase 则更新为 aborted|paused
            int statusIngCount = autoexecJobMapper.getJobPhaseRunnerCountByJobIdAndRunnerStatus(jobId, statusIng);
            if (statusIngCount == 0) {
                jobVo.setStatus(status);
                autoexecJobMapper.updateJobStatus(jobVo);
                //将中止中和暂停中的phase 状态更新为 已中止和已暂停
                autoexecJobMapper.updateJobPhaseStatusByJobIdAndPhaseStatus(jobId, statusIng, status);
            }
            //}
        } else {
            jobVo.setStatus(status);
            autoexecJobMapper.updateJobStatus(jobVo);
        }


        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/status/update";
    }
}
