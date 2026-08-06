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
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeSearchApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmaa.autoexecjobphasenodesearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "term.autoexec.jobphaseid", isRequired = true),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.jobstatuslist"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmaa.common.input.param.desc.nodekeyword", xss = true),
            @Param(name = "isDelete", type = ApiParamType.STRING, desc = "nmaa.common.input.param.desc.isdeleted"),
            @Param(name = "nodeIdList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecjobphasenodesearchapi.input.param.desc.nodeidlist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobPhaseNodeVo[].class, desc = "nmaa.common.output.param.desc.datalist"),
            @Param(name = "status", type = ApiParamType.STRING, desc = "term.autoexec.jobstatuslabel"),
            @Param(name = "statusName", type = ApiParamType.STRING, desc = "term.autoexec.jobstatusname"),
            @Param(explode = BasePageVo.class),
    })
    @Description(desc = "nmaa.autoexecjobphasenodesearchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecJobPhaseNodeVo jobPhaseNodeVo = JSONObject.toJavaObject(jsonObj, AutoexecJobPhaseNodeVo.class);
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId());
        if (phaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobPhaseNodeVo.getJobPhaseId().toString());
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(phaseVo.getJobId().toString());
        }
        List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = new ArrayList<>();
        int rowNum = autoexecJobMapper.searchJobPhaseNodeCount(jobPhaseNodeVo);
        jobPhaseNodeVo.setRowNum(rowNum);
        if (rowNum > 0) {
            jobPhaseNodeVoList = autoexecJobMapper.searchJobPhaseNodeWithResource(jobPhaseNodeVo);
        }
        // 补充剧本节点额外信息
        AutoexecJobInvokeVo invokeVo = autoexecJobMapper.getJobInvokeByJobId(phaseVo.getJobId());
        if (invokeVo != null) {
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(invokeVo.getType());
            if (autoexecJobSourceActionHandler != null) {
                autoexecJobSourceActionHandler.addExtraJobPhaseNodeInfoByList(phaseVo.getJobId(), jobPhaseNodeVoList);
            }
        }
        JSONObject result = TableResultUtil.getResult(jobPhaseNodeVoList, jobPhaseNodeVo);
        result.put("status", jobVo.getStatus());
        result.put("statusName", JobStatus.getText(jobVo.getStatus()));
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/search";
    }


}
