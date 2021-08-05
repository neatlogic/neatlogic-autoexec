/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobNodeSqlVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/8/5 11:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobNodeSqlListApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService jobService;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "获取作业节点sql列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id")
    })
    @Output({
    })
    @Description(desc = "获取作业节点sql列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        Long phaseId = paramObj.getLong("jobPhaseId");
        Long resourceId = paramObj.getLong("resourceId");
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(phaseId,resourceId);
        if (nodeVo == null) {
            throw new AutoexecJobPhaseNodeNotFoundException(phaseId.toString(), resourceId == null? StringUtils.EMPTY:resourceId.toString());
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(nodeVo.getJobId(), nodeVo.getJobPhaseId());
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
        List<AutoexecJobNodeSqlVo> sqlList = autoexecJobActionService.getNodeSqlList(paramObj);
        if(CollectionUtils.isNotEmpty(sqlList)&&sqlList.stream().allMatch(o->Objects.equals(o.getStatus(),JobNodeStatus.SUCCEED.getValue()))){
            result.put("isRefresh", 0);
        }
        result.put("nodeSqlList",sqlList);
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/node/sql/list";
    }
}
