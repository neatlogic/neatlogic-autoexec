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
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeStatusCountVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static neatlogic.framework.common.util.CommonUtil.distinctByKey;

/**
 * @author lvzk
 * @since 2022/5/6 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseListApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "nmaa.autoexecjobphaselistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "phaseIdList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecjobphaselistapi.input.param.desc.phaseidlist"),
    })
    @Output({
            @Param(name = "status", type = ApiParamType.STRING, desc = "term.autoexec.jobstatuslabel"),
            @Param(name = "statusName", type = ApiParamType.STRING, desc = "term.autoexec.jobstatusname"),
            @Param(name = "phaseList", explode = AutoexecJobPhaseVo[].class, desc = "nmaa.autoexecjobphaselistapi.output.param.desc.phaselist"),
    })
    @Description(desc = "nmaa.autoexecjobphaselistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        List<Long> jobPhaseIdList = null;
        List<AutoexecJobPhaseVo> jobPhaseVoList;
        if (jsonObj.containsKey("phaseIdList")) {
            jobPhaseIdList = jsonObj.getJSONArray("phaseIdList").toJavaList(Long.class);
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (CollectionUtils.isEmpty(jobPhaseIdList)) {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupAndRunnerByJobId(jobId);
        } else {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupAndRunnerByJobIdAndPhaseIdList(jobId, jobPhaseIdList);
        }

        //过滤出需要根据入参phaseList 更新执行目标的阶段List
        List<AutoexecJobPhaseVo> jobPrPhaseList = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getIsPreOutputUpdateNode(), 1) && Objects.equals(o.getStatus(), JobPhaseStatus.COMPLETED.getValue())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(jobPrPhaseList)) {
            for (AutoexecJobPhaseVo prePhase : jobPrPhaseList) {
                jobPhaseVoList.addAll(autoexecJobService.getJobPhaseListByPreOutput(jobVo, prePhase));
            }
            jobPhaseVoList = jobPhaseVoList.stream().filter(distinctByKey(AutoexecJobPhaseVo::getId)).collect(Collectors.toList());
        }

        List<AutoexecJobPhaseNodeStatusCountVo> statusCountVoList = autoexecJobMapper.getJobPhaseNodeStatusCount(jobId);
        for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
            for (AutoexecJobPhaseNodeStatusCountVo statusCountVo : statusCountVoList) {
                if (statusCountVo.getJobPhaseId().equals(phaseVo.getId())) {
                    phaseVo.addStatusCountVo(statusCountVo);
                }
            }
            List<AutoexecJobPhaseNodeStatusCountVo> jobPhaseNodeStatusCountVoList = phaseVo.getStatusCountVoList();
            AtomicInteger succeedCount = new AtomicInteger(0);
            AtomicInteger totalCount = new AtomicInteger();
            jobPhaseNodeStatusCountVoList.forEach(o -> {
                if (Arrays.asList(JobNodeStatus.SUCCEED.getValue(),JobNodeStatus.IGNORED.getValue()).contains(o.getStatus())) {
                    succeedCount.set(succeedCount.get()+o.getCount());

                }
                totalCount.addAndGet(o.getCount());
            });
            phaseVo.setCompletionRate((int) (Double.parseDouble(Integer.toString(succeedCount.get())) / Double.parseDouble(Integer.toString(totalCount.get())) * 100));
        }
        result.put("status", jobVo.getStatus());
        result.put("statusName", JobStatus.getText(jobVo.getStatus()));
        result.put("phaseList", jobPhaseVoList);
        IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
        if (jobSource == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler jobSourceTypeHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
        if (jobSourceTypeHandler != null) {
            result.put("extraInfo",jobSourceTypeHandler.getExtraRefreshJobInfo(jobVo));
        }
        try {
            result.put("waitingDetail", autoexecJobService.getAutoexecJobWaitingDetail(jobVo.getId()));
        }catch (Exception ignored){
            //ignored
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/list";
    }
}
