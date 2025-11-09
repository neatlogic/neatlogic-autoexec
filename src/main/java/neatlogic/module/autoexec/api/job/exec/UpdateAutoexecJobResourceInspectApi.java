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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/9/16 14:15
 **/
@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobResourceInspectApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "更新巡检资源作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id", isRequired = true),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
            @Param(name = "inspectTime", type = ApiParamType.LONG, desc = "巡检时间", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "更新巡检资源作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String phaseName = jsonObj.getString("phaseName");
        Long jobId = jsonObj.getLong("jobId");
        Long phaseId = null;
        //TODO 临时允许phaseName 为空
        if(StringUtils.isNotBlank(phaseName)) {
            AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phaseName);
            if (phaseVo == null) {
                throw new AutoexecJobPhaseNotFoundException(phaseName);
            }
            phaseId = phaseVo.getId();
        }
        autoexecJobMapper.insertDuplicateJobResourceInspect(jobId,jsonObj.getLong("resourceId"),phaseId,jsonObj.getDate("inspectTime"));
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/resource/inspect/update";
    }
}
