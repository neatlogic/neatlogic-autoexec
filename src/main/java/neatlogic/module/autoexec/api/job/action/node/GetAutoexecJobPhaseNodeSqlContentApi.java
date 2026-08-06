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
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lvzk
 * @since 2021/8/5 11:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseNodeSqlContentApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "nmaa.getautoexecjobphasenodesqlcontentapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.jobphaseid"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "term.cmdb.resourceid"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "term.autoexec.jobsqlname"),
            @Param(name = "encoding", type = ApiParamType.STRING, desc = "term.autoexec.encoding", defaultValue = "UTF-8")
    })
    @Output({
    })
    @Description(desc = "nmaa.getautoexecjobphasenodesqlcontentapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.GET_NODE_SQL_CONTENT.getValue());
        IAutoexecJobActionHandler getNodeSqlContentAction = AutoexecJobActionHandlerFactory.getAction(JobAction.GET_NODE_SQL_CONTENT.getValue());
        return getNodeSqlContentAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/sql/content/get";
    }
}
