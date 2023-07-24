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

package neatlogic.module.autoexec.api.job;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.job.AutoexecJobNotFoundEditTargetException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/5/11 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobInfoGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmaaj.autoexecjobinfogetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true)
    })
    @Output({
            @Param(explode = AutoexecJobVo.class, desc = "term.autoexec.jobinfo"),
    })
    @Description(desc = "nmaaj.autoexecjobinfogetapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundEditTargetException(jobId);
        }
        //判断是否有执行与接管权限
        if(!Objects.equals(jobVo.getStatus(), JobStatus.CHECKED.getValue())) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            autoexecJobSourceActionHandler.getJobActionAuth(jobVo);
        }
        //补充作业额外信息，如发布
        AutoexecJobInvokeVo invokeVo = autoexecJobMapper.getJobInvokeByJobId(jobId);
        if(invokeVo != null) {
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(invokeVo.getType());
            if (autoexecJobSourceActionHandler != null) {
                jobVo.setExtraInfo(autoexecJobSourceActionHandler.getExtraJobInfo(jobVo));
            }
        }
        return jobVo;
    }

    @Override
    public String getToken() {
        return "autoexec/job/info/get";
    }
}
