/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.autoexec.job.action.handler;

import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobFireHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.FIRE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        //List<AutoexecJobPhaseVo> executePhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobVo.getId(),jobVo.getExecuteJobGroupVo().getSort());
        //return Objects.equals(JobStatus.PENDING.getValue(), jobVo.getStatus()) || executePhaseVoList.stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.PENDING.getValue()));
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck(){
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        autoexecJobMapper.getJobLockByJobId(jobVo.getId());
        autoexecJobService.executeGroup(jobVo);
        return new JSONObject(){{
            put("jobId",jobVo.getId());
        }};
    }
}
