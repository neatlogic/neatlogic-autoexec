/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobNodeResetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "重置作业节点";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.STRING, desc = "作业阶段Id", isRequired = true),
            @Param(name = "nodeIdList", type = ApiParamType.JSONARRAY, desc = "作业节点idList"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, desc = "是否全部重置,1:是 0:否,则nodeIdList不能为空", isRequired = true),

    })
    @Output({
    })
    @Description(desc = "重置作业节点")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long jobPhaseId = jsonObj.getLong("jobPhaseId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if(jobVo == null){
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(jobId,jobPhaseId);
        if(phaseVo == null){
            throw new AutoexecJobPhaseNotFoundException(jobPhaseId.toString());
        }
        List<Long> nodeIdList = JSONObject.parseArray(jsonObj.getJSONArray("nodeIdList").toJSONString(), Long.class);
        autoexecJobActionService.executeAuthCheck(jobVo);
        jobVo.setAction(JobAction.RESET_NODE.getValue());
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByNodeIdList(nodeIdList);
        if (CollectionUtils.isEmpty(nodeVoList)) {
            throw new AutoexecJobPhaseNodeNotFoundException(StringUtils.EMPTY, nodeIdList.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        jobVo.setJobPhaseNodeList(nodeVoList);
        jobVo.setPhaseList(Collections.singletonList(phaseVo));
        autoexecJobActionService.resetNode(jobVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/node/reset";
    }
}
