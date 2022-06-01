/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/5/6 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeListApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取作业阶段节点列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "jobPhaseStatus", type = ApiParamType.STRING, desc = "作业阶段状态", isRequired = true),
            @Param(name = "nodeIdList", type = ApiParamType.JSONARRAY, desc = "作业阶段节点idList"),
    })
    @Output({
            @Param(name = "status", type = ApiParamType.STRING, desc = "作业状态"),
            @Param(name = "statusName", type = ApiParamType.STRING, desc = "作业状态名"),
            @Param(name = "nodeList", explode = AutoexecJobPhaseNodeVo[].class, desc = "作业阶段节点list"),
    })
    @Description(desc = "获取作业阶段节点列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        List<Long> nodeIdList = null;
        if (jsonObj.containsKey("nodeIdList")) {
            nodeIdList = jsonObj.getJSONArray("nodeIdList").toJavaList(Long.class);
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        result.put("status", jobVo.getStatus());
        result.put("statusName", JobStatus.getText(jobVo.getStatus()));
        Long phaseId = null;
        //判断前端是否需要继续刷新
        int isRefresh = 0;
        String jobPhaseStatusOld = jsonObj.getString("jobPhaseStatus");
        boolean nodeNeedRefresh = true;
        if (CollectionUtils.isNotEmpty(nodeIdList)) {
            List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByNodeIdList(nodeIdList);
            if (CollectionUtils.isNotEmpty(nodeVoList)) {
                phaseId = nodeVoList.get(0).getJobPhaseId();
                AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(phaseId);
                //node存在失败的节点，无需刷新
                if (nodeVoList.stream().anyMatch(o -> Arrays.asList(JobNodeStatus.FAILED.getValue(), JobNodeStatus.ABORTED.getValue()).contains(o.getStatus()))) {
                    nodeNeedRefresh = false;
                }
                //node如果都是succeed｜ignored，无需刷新
                if (nodeVoList.stream().allMatch(o -> Arrays.asList(JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue()).contains(o.getStatus()))) {
                    nodeNeedRefresh = false;
                }
                //phase是pending，无需刷新
                if (Objects.equals(JobPhaseStatus.PENDING.getValue(), phaseVo.getStatus())) {
                    nodeNeedRefresh = false;
                }
                //phase如果是running，需刷新
                if (nodeNeedRefresh || (Objects.equals(JobPhaseStatus.RUNNING.getValue(), jobPhaseStatusOld) || Objects.equals(JobPhaseStatus.RUNNING.getValue(), phaseVo.getStatus()))
                ) {
                    isRefresh = 1;
                }
            }
            result.put("nodeList", nodeVoList);
        }
        result.put("isRefresh", isRefresh);
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/list";
    }
}
