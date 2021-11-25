/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobNodeSqlVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeSqlListGetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeSqlListGetHandler.class);

    @Override
    public String getName() {
        return JobAction.GET_NODE_SQL_LIST.getValue();
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getPhaseList().get(0);
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("nodeId", nodeVo.getId());
        paramObj.put("resourceId", nodeVo.getResourceId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        paramObj.put("phaseId", nodeVo.getJobPhaseId());
        paramObj.put("ip", nodeVo.getHost());
        paramObj.put("port", nodeVo.getPort());
        paramObj.put("runnerUrl", nodeVo.getRunnerUrl());
        paramObj.put("execMode", phaseVo.getExecMode());

        result.put("isRefresh", 1);
        String url = paramObj.getString("runnerUrl") + "/api/rest/job/phase/node/sql/list";
        JSONArray sqlArray = JSONObject.parseArray(requestRunner(url, paramObj));
        List<AutoexecJobNodeSqlVo> sqlList =  sqlArray.toJavaList(AutoexecJobNodeSqlVo.class);
        if(Objects.equals(paramObj.getString("status"),JobNodeStatus.SUCCEED.getValue())&&CollectionUtils.isNotEmpty(sqlList)&&sqlList.stream().allMatch(o->Objects.equals(o.getStatus(),JobNodeStatus.SUCCEED.getValue()))){
            result.put("isRefresh", 0);
        }
        result.put("nodeSqlList",sqlList);
        return result;
    }
}
