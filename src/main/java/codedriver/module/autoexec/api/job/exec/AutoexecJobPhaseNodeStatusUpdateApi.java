/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    static Logger logger = LoggerFactory.getLogger(AutoexecJobPhaseNodeStatusUpdateApi.class);
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
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "节点Id"),
            @Param(name = "host", type = ApiParamType.STRING, desc = "节点ip"),
            @Param(name = "port", type = ApiParamType.STRING, desc = "节点port"),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true),
            @Param(name = "failIgnore", type = ApiParamType.INTEGER, desc = "失败是否继续，1：继续 0：停止", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数")
    })
    @Output({
    })
    @Description(desc = "回调更新作业剧本节点状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        JSONObject result = new JSONObject();
        Long nodeId = jsonObj.getLong("nodeId");
        if (nodeId != null && nodeId > 0) {
            Integer failIgnore = jsonObj.getInteger("failIgnore");
            String phaseName = jsonObj.getString("phase");
            AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jsonObj.getLong("jobId"), phaseName);
            if (jobPhaseVo == null) {
                throw new AutoexecJobPhaseNotFoundException(phaseName);
            }
            AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobNodeId(nodeId);
            if (nodeVo == null) {
                throw new AutoexecJobPhaseNodeNotFoundException(phaseName, nodeId);
            }
            nodeVo.setStatus(jsonObj.getString("status"));
            //如果节点失败且failIgnore等于0，则表明失败中止;如果节点成功，则需要查询是否存在失败的phase
            if (Objects.equals(nodeVo.getStatus(), JobNodeStatus.FAILED.getValue()) && Objects.equals(failIgnore, 0)) {
                autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(nodeVo.getJobId(), nodeVo.getJobPhaseName());
                jobPhaseVo.setStatus(JobPhaseStatus.FAILED.getValue());
                autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(nodeVo.getJobPhaseId(), nodeVo.getStatus()));
            }
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/status/update";
    }
}
