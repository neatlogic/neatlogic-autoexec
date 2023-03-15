/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.job.action.handler.node;

import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeAuditListHandler extends AutoexecJobActionHandlerBase {
    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return JobAction.NODE_AUDIT_LIST.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("sqlName", paramObj.getString("sqlName"));
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());
        String url = paramObj.getString("runnerUrl") + "/api/rest/job/phase/node/execute/audit/list";
        List<AutoexecJobPhaseNodeAuditVo> auditList = new ArrayList<>();
        JSONArray auditArray = JSONArray.parseArray(AutoexecUtil.requestRunner(url, paramObj));
        for (Object audit : auditArray) {
            JSONObject auditJson = (JSONObject) audit;
            AutoexecJobPhaseNodeAuditVo auditVo = new AutoexecJobPhaseNodeAuditVo(auditJson);
            UserVo execUserVo = userMapper.getUserBaseInfoByUuidWithoutCache(auditVo.getExecUser());
            if(execUserVo == null){
                execUserVo = new UserVo(auditVo.getExecUser());
            }
            auditVo.setExecUserVo(execUserVo);
            auditList.add(auditVo);
        }
        result.put("tbodyList", auditList.stream().sorted(Comparator.comparing(AutoexecJobPhaseNodeAuditVo::getStartTime).reversed()).collect(Collectors.toList()));
        return result;
    }
}