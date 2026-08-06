/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.job.action.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/21 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SubmitAutoexecJobPhaseNodeWaitInputApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getName() {
        return "nmaa.submitautoexecjobphasenodewaitinputapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.STRING, desc = "term.autoexec.jobphaseid", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "term.cmdb.resourceid"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "term.autoexec.jobsqlname"),
            @Param(name = "option", type = ApiParamType.STRING, desc = "nmaa.submitautoexecjobphasenodewaitinputapi.input.param.desc.option", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "nmaa.submitautoexecjobphasenodewaitinputapi.getname")
    @ResubmitInterval(value = 5)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.SUBMIT_NODE_WAIT_INPUT.getValue());
        IAutoexecJobActionHandler submitNodeWaitInputAction = AutoexecJobActionHandlerFactory.getAction(JobAction.SUBMIT_NODE_WAIT_INPUT.getValue());
        submitNodeWaitInputAction.doService(jobVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/submit/waitInput";
    }
}
