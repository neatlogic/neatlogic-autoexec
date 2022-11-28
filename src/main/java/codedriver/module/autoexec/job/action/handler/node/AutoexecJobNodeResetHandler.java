/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeResetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeResetHandler.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.RESET_NODE.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        return true;
    }


    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        List<AutoexecJobPhaseNodeVo> nodeVoList;
        //更新状态
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getCurrentPhase();
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            jobVo.getActionParam().put("phaseName", currentPhaseVo.getName());
            IAutoexecJobSourceTypeHandler handler;
            if (StringUtils.equals(jobVo.getSource(), JobSourceType.DEPLOY.getValue())) {
                handler = AutoexecJobSourceTypeHandlerFactory.getAction(JobSourceType.DEPLOY.getValue());
            } else {
                handler = AutoexecJobSourceTypeHandlerFactory.getAction(codedriver.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue());
            }
            handler.resetSqlStatus(jobVo.getActionParam(), jobVo);
        } else {
            Integer isAll = jobVo.getActionParam().getInteger("isAll");
            if (!Objects.equals(isAll, 1)) {
                currentResourceIdListValid(jobVo);
                //如果勾选的节点已经是所有的节点，则也需要重置phase的状态为pending
                if (jobVo.getExecuteJobNodeVoList().size() == autoexecJobMapper.getJobPhaseNodeCountWithoutDeleteByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhaseId())) {
                    autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(Collections.singletonList(jobVo.getCurrentPhaseId()), JobPhaseStatus.PENDING.getValue());
                }
                //重置节点 (status、startTime、endTime)
                autoexecJobMapper.updateJobPhaseNodeListStatus(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());
                jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList())));
            } else {
                autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(Collections.singletonList(currentPhaseVo.getId()), JobPhaseStatus.PENDING.getValue());
                autoexecJobMapper.updateJobPhaseNodeStatusByJobPhaseIdAndIsDelete(currentPhaseVo.getId(), JobNodeStatus.PENDING.getValue(), 0);
                //jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeListWithoutDeleteByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhaseId()));
            }

        }
        autoexecJobService.resetJobNodeStatus(jobVo);
        return null;
    }
}
