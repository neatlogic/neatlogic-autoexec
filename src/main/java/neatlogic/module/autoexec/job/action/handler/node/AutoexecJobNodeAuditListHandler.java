/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
