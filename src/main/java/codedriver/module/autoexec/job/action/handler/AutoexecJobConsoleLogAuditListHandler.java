/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobConsoleLogAuditVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectRefusedException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AutoexecJobConsoleLogAuditListHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobConsoleLogAuditListHandler.class);
    @Resource
    RunnerMapper runnerMapper;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return JobAction.CONSOLE_LOG_AUDIT_LIST.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        Long runnerId = jobVo.getActionParam().getLong("runnerId");
        RunnerVo runnerVo = runnerMapper.getRunnerById(runnerId);
        if (runnerVo == null) {
            throw new AutoexecJobRunnerNotFoundException(runnerId.toString());
        }
        jobVo.getActionParam().put("runnerUrl", runnerVo.getUrl());
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) throws Exception {
        JSONObject result = new JSONObject();
        JSONObject paramObj = jobVo.getActionParam();
        String url = paramObj.getString("runnerUrl") + "/api/rest/job/console/log/audit/list";
        HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        if(StringUtils.isNotBlank(requestUtil.getError())){
            throw new AutoexecJobRunnerConnectRefusedException(url);
        }
        List<AutoexecJobConsoleLogAuditVo> auditList = new ArrayList<>();
        JSONArray auditArray = requestUtil.getResultJsonArray();
        for (int i = 0; i < auditArray.size(); i++) {
            JSONObject auditJson = auditArray.getJSONObject(i);
            AutoexecJobConsoleLogAuditVo autoexecJobConsoleLogAuditVo = new AutoexecJobConsoleLogAuditVo(auditJson);
            UserVo execUserVo = userMapper.getUserBaseInfoByUuidWithoutCache(autoexecJobConsoleLogAuditVo.getExecUser());
            if(execUserVo == null){
                execUserVo = new UserVo(autoexecJobConsoleLogAuditVo.getExecUser());
            }
            autoexecJobConsoleLogAuditVo.setExecUserVo(execUserVo);
            auditList.add(autoexecJobConsoleLogAuditVo);
        }
        result.put("tbodyList", auditList.stream().sorted(Comparator.comparing(AutoexecJobConsoleLogAuditVo::getStartTime).reversed()).collect(Collectors.toList()));
        return  result;
    }
}
