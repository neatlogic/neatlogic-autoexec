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

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dto.job.AutoexecJobSyncVo;
import neatlogic.module.autoexec.job.sync.AutoexecJobSyncManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecCombopJobSyncApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobSyncManager autoexecJobSyncManager;

    @Override
    public String getName() {
        return "nmaaj.getautoexeccombopjobsyncapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
            @Param(name = "listeningCount", type = ApiParamType.INTEGER, desc = "nmaaj.getautoexeccombopjobsyncapi.output.listeningcount"),
            @Param(name = "maxListeningCount", type = ApiParamType.INTEGER, desc = "nmaaj.getautoexeccombopjobsyncapi.output.maxlisteningcount"),
            @Param(name = "availableCount", type = ApiParamType.INTEGER, desc = "nmaaj.getautoexeccombopjobsyncapi.output.availablecount"),
            @Param(name = "jobList", type = ApiParamType.JSONARRAY, explode = AutoexecJobSyncVo[].class, desc = "nmaaj.getautoexeccombopjobsyncapi.output.joblist"),
            @Param(name = "serverId", type = ApiParamType.INTEGER, desc = "nmaaj.getautoexeccombopjobsyncapi.output.serverid"),
            @Param(name = "scope", type = ApiParamType.STRING, desc = "nmaaj.getautoexeccombopjobsyncapi.output.scope")
    })
    @Description(desc = "nmaaj.getautoexeccombopjobsyncapi.description.desc")
    @Override
    public Object myDoService(JSONObject paramObj) {
        String tenantUuid = TenantContext.get().getTenantUuid();
        int listeningCount = autoexecJobSyncManager.getSyncCount(tenantUuid);
        int maxListeningCount = autoexecJobSyncManager.getMaxConcurrent();
        JSONObject result = new JSONObject();
        result.put("listeningCount", listeningCount);
        result.put("maxListeningCount", maxListeningCount);
        result.put("availableCount", Math.max(0, maxListeningCount - listeningCount));
        result.put("jobList", autoexecJobSyncManager.getSyncJobList(tenantUuid));
        result.put("serverId", Config.SCHEDULE_SERVER_ID);
        result.put("scope", "instance");
        return result;
    }

    @Override
    public String getToken() {
        return "/autoexec/combop/job/sync/get";
    }
}
