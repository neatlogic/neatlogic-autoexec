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

package neatlogic.module.autoexec.process.audithandler.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.process.audithandler.core.IProcessTaskStepAuditDetailHandler;
import neatlogic.framework.process.dto.ProcessTaskStepAuditDetailVo;
import neatlogic.module.autoexec.process.constvalue.AutoexecProcessTaskAuditDetailType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AutoexecSuccessMessageAuditHandler implements IProcessTaskStepAuditDetailHandler {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getType() {
        return AutoexecProcessTaskAuditDetailType.AUTOEXECMESSAGE.getValue();
    }

    @Override
    public int handle(ProcessTaskStepAuditDetailVo processTaskStepAuditDetailVo) {
        JSONObject resultObj = new JSONObject();
        String newContent = processTaskStepAuditDetailVo.getNewContent();
        if (StringUtils.isNotBlank(newContent)) {
            JSONObject jsonObj = JSONObject.parseObject(newContent);
            if (MapUtils.isNotEmpty(jsonObj)) {
                JSONArray jobIdArray = jsonObj.getJSONArray("jobIdList");
                if (CollectionUtils.isNotEmpty(jobIdArray)) {
                    List<Long> jobIdList = jobIdArray.toJavaList(Long.class);
                    int completed = 0, failed = 0, running = 0;
                    Map<Long, List<AutoexecJobPhaseVo>> jobIdToAutoexecJobPhaseListMap = new HashMap<>();
                    List<AutoexecJobPhaseVo> jobPhaseList = autoexecJobMapper.getJobPhaseListWithGroupByJobIdList(jobIdList);
                    for (AutoexecJobPhaseVo autoexecJobPhaseVo : jobPhaseList) {
                        jobIdToAutoexecJobPhaseListMap.computeIfAbsent(autoexecJobPhaseVo.getJobId(), key -> new ArrayList<>()).add(autoexecJobPhaseVo);
                    }
                    List<AutoexecJobVo> autoexecJobList = autoexecJobMapper.getJobListByIdList(jobIdList);
                    for (AutoexecJobVo autoexecJobVo : autoexecJobList) {
                        List<AutoexecJobPhaseVo> jobPhaseVoList = jobIdToAutoexecJobPhaseListMap.get(autoexecJobVo.getId());
                        autoexecJobVo.setPhaseList(jobPhaseVoList);
                        if (JobStatus.isRunningStatus(autoexecJobVo.getStatus())) {
                            running++;
                        } else if (JobStatus.isCompletedStatus(autoexecJobVo.getStatus())) {
                            completed++;
                        } else if (JobStatus.isFailedStatus(autoexecJobVo.getStatus())) {
                            failed++;
                        }
                    }

                    if (running > 0) {
                        resultObj.put("status", JobStatus.RUNNING.getValue());
                    } else if (failed > 0) {
                        resultObj.put("status", JobStatus.FAILED.getValue());
                    } else if (completed > 0) {
                        resultObj.put("status", JobStatus.COMPLETED.getValue());
                    }
                    resultObj.put("jobList", autoexecJobList);
                    processTaskStepAuditDetailVo.setNewContent(resultObj.toJSONString());
                }
                return 1;
            }
        }
        return 0;
    }
}
