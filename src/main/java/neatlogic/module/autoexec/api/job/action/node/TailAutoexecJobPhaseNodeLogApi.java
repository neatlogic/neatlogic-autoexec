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
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/6 14:49
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TailAutoexecJobPhaseNodeLogApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "nmaa.tailautoexecjobphasenodelogapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.jobphaseid"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "term.cmdb.resourceid"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "term.autoexec.jobsqlname"),
            @Param(name = "status", type = ApiParamType.STRING, isRequired = true, desc = "nmaa.common.input.param.desc.refreshstatus"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.tailautoexecjobphasenodelogapi.input.param.desc.logpos"),
            @Param(name = "direction", type = ApiParamType.ENUM, rule = "up,down", isRequired = true, desc = "nmaa.common.input.param.desc.logdirection"),
            @Param(name = "encoding", type = ApiParamType.STRING, desc = "term.autoexec.encoding", defaultValue = "UTF-8")
    })
    @Output({
            @Param(name = "tailContent", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.common.output.param.desc.tailcontent"),
            @Param(name = "startPos", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.common.output.param.desc.logstartposition"),
            @Param(name = "endPos", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.common.output.param.desc.logendposition"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.common.output.param.desc.logposition"),
            @Param(name = "last", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.common.output.param.desc.logcontent"),
            @Param(name = "interact", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "nmaa.tailautoexecjobphasenodelogapi.output.param.desc.interact"),
            @Param(name = "isRefresh", type = ApiParamType.INTEGER, isRequired = true, desc = "nmaa.common.output.param.desc.isrefresh")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        autoexecJobService.validateAutoexecJobLogEncoding(paramObj.getString("encoding"));
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.TAIL_NODE_LOG.getValue());
        IAutoexecJobActionHandler tailNodeLogAction = AutoexecJobActionHandlerFactory.getAction(JobAction.TAIL_NODE_LOG.getValue());
        return tailNodeLogAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "/autoexec/job/phase/node/log/tail";
    }
}
