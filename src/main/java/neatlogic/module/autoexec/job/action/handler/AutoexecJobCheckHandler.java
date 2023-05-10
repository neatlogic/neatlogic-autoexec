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
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/8/11 12:18
 **/
@Service
public class AutoexecJobCheckHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return JobAction.CHECK.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        if (Objects.equals(jobVo.getStatus(), JobStatus.COMPLETED.getValue())) {
            jobVo.setStatus(JobStatus.CHECKED.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
        }
        return null;
    }
}
