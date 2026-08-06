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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 仅允许phase 和 node 状态都不是running的情况下才能执行重跑动作
 *
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class IgnoreAutoexecJobPhaseNodeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmaa.ignoreautoexecjobphasenodeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.STRING, desc = "term.autoexec.jobphaseid", isRequired = true),
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.resourceidlist"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, desc = "nmaa.ignoreautoexecjobphasenodeapi.input.param.desc.isall"),
    })
    @Output({
    })
    @Description(desc = "nmaa.ignoreautoexecjobphasenodeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray resourceIdList = jsonObj.getJSONArray("resourceIdList");
        Integer isAll = jsonObj.getInteger("isAll");
        if(CollectionUtils.isEmpty(resourceIdList) && isAll == null) {
            throw new ParamIrregularException("resourceIdList | isAll");
        }
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        jobVo.setActionParam(jsonObj);
        jobVo.setAction(JobAction.IGNORE_NODE.getValue());
        IAutoexecJobActionHandler ignoreNodeAction = AutoexecJobActionHandlerFactory.getAction(JobAction.IGNORE_NODE.getValue());
        return ignoreNodeAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/ignore";
    }
}
