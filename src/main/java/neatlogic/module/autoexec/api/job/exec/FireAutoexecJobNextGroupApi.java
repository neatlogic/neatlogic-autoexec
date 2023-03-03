/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.job.exec;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobGroupVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
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
public class FireAutoexecJobNextGroupApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "激活作业下一组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "groupNo", type = ApiParamType.STRING, desc = "上一个组（排序）序号", isRequired = true),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "runnerId", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数", isRequired = true),
            @Param(name = "time", type = ApiParamType.DOUBLE, desc = "回调时间")
    })
    @Output({
    })
    @Description(desc = "激活作业下一组")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Integer groupSort = jsonObj.getInteger("groupNo");
        Long runnerId = jsonObj.getLong("runnerId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        jobVo.setPassThroughEnv(jsonObj.getJSONObject("passThroughEnv"));
        autoexecJobActionService.initExecuteUserContext(jobVo);

        //更新group对应runner的"是否fireNext"标识为1
        autoexecJobMapper.updateJobPhaseRunnerFireNextByJobIdAndGroupSortAndRunnerId(jobId, groupSort, 1, runnerId);
        /*
         *判断是否满足激活下个phase条件
         * 1、当前sort的所有phase都completed,
         * 2、当前sort的所有phase的runner 都是completed，所有runner的"是否fireNext"标识都为1
         */
        if (autoexecJobService.checkIsAllActivePhaseIsCompleted(jobId, groupSort)) {
            if(Objects.equals(JobStatus.ABORTING.getValue(),jobVo.getStatus())){
                jobVo.setStatus(JobStatus.ABORTED.getValue());
                autoexecJobMapper.updateJobStatus(jobVo);
            }else {
                AutoexecJobGroupVo nextGroupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobId, groupSort + 1);
                if (nextGroupVo != null) {
                    jobVo.setExecuteJobGroupVo(nextGroupVo);
                    IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                    fireAction.doService(jobVo);
                }
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/next/group/fire";
    }
}
