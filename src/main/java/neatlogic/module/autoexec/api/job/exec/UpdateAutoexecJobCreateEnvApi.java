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

package neatlogic.module.autoexec.api.job.exec;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobEnvVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/9/16 14:15
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobCreateEnvApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "跟新作业环境变量（出参）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "作业环境变量名", isRequired = true),
            @Param(name = "envValue", type = ApiParamType.STRING, desc = "作业环境变量值", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "跟新作业环境变量（出参）接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecJobEnvVo jobEnvVo = new AutoexecJobEnvVo(jsonObj.getLong("jobId"), jsonObj.getString("envName"), jsonObj.getString("envValue"));
        autoexecJobMapper.insertDuplicateJobEnv(jobEnvVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/env/update";
    }
}
