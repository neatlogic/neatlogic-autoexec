/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
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
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "host", type = ApiParamType.STRING, desc = "节点ip"),
            @Param(name = "port", type = ApiParamType.STRING, desc = "节点port"),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true),
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
        Long resourceId = jsonObj.getLong("resourceId");
        String phaseName = jsonObj.getString("phase");
        AutoexecJobPhaseNodeVo nodeVo = null;
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jsonObj.getLong("jobId"), phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(phaseName);
        }
        //获取node
        if(Objects.equals(jobPhaseVo.getExecMode(), ExecMode.RUNNER.getValue())){
            List<AutoexecJobPhaseNodeVo> nodeList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobId,jobPhaseVo.getId());
            if(CollectionUtils.isNotEmpty(nodeList)) {
                nodeVo = nodeList.get(0);
            }
        }else {
            nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobIdAndJobPhaseNameAndResourceId(jobId, phaseName, resourceId);
        }
        //不抛异常影响其它节点运行，ignore 就好
        if (nodeVo == null) {
            return null;
        }

        //如果node status 和原本的status 或者  warnCount 一样则无需更新
        if (!Objects.equals(nodeVo.getStatus(), jsonObj.getString("status")) || !Objects.equals(nodeVo.getWarnCount(), jsonObj.getInteger("warnCount"))) {
            nodeVo.setStatus(jsonObj.getString("status"));
            nodeVo.setWarnCount(jsonObj.getInteger("warnCount"));
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/status/update";
    }
}
