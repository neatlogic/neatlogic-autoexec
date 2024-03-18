/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.job.action.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobConsoleLogAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.exception.runner.RunnerConnectRefusedException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
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
        HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setConnectTimeout(5000).setReadTimeout(5000).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        if(StringUtils.isNotBlank(requestUtil.getError())){
            throw new RunnerConnectRefusedException(url);
        }
        List<AutoexecJobConsoleLogAuditVo> auditList = new ArrayList<>();
        JSONObject resultJson = requestUtil.getResultJson();
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new RunnerHttpRequestException(resultJson.getString("Message"));
        }
        JSONArray auditArray = resultJson.getJSONArray("Return");
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
