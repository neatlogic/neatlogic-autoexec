/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.callback;

import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobPhaseNodeStatusUpdateApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "回调更新作业剧本节点状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "preJobId", type = ApiParamType.LONG, desc = "上一个作业Id"),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数"),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "operationId", type = ApiParamType.LONG, desc = "作业剧本Name", isRequired = true),
            @Param(name = "node", type = ApiParamType.JSONOBJECT, desc = "执行完的节点", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "回调更新作业剧本节点状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long operationId = jsonObj.getLong("operationId");
        AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(jsonObj);
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(nodeVo.getJobId(), nodeVo.getJobPhaseName());
        nodeVo.setJobPhaseId(jobPhaseVo.getId());
        if (autoexecJobMapper.checkIsJobPhaseNodeExist(nodeVo) == 0) {
            throw new AutoexecJobPhaseNodeNotFoundException(nodeVo.getJobPhaseName(), nodeVo.getHost() + ":" + nodeVo.getPort());
        }
        AutoexecJobPhaseOperationVo operationVo = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseIdAndOperationId(nodeVo.getJobId(), nodeVo.getJobPhaseId(), operationId);
        if (Objects.equals(nodeVo.getStatus(), JobNodeStatus.FAILED) )
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);


        result.put("phaseStatus", JobPhaseStatus.FAILED.getValue());
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/status/update";
    }
}
