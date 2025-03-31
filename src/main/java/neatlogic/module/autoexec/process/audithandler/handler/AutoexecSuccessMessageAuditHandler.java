/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
