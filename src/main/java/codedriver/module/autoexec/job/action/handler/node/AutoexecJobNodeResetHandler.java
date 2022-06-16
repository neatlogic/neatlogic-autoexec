/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerFactory;
import codedriver.framework.autoexec.job.source.action.IAutoexecJobSourceActionHandler;
import codedriver.framework.deploy.constvalue.DeployOperType;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
            IAutoexecJobSourceActionHandler handler;
            if (StringUtils.equals(jobVo.getSource(), DeployOperType.DEPLOY.getValue())) {
                handler = AutoexecJobSourceActionHandlerFactory.getAction(DeployOperType.DEPLOY.getValue());
            } else {
                handler = AutoexecJobSourceActionHandlerFactory.getAction(AutoexecOperType.AUTOEXEC.getValue());
            }
            handler.resetSqlStatus(jobVo.getActionParam(), jobVo);
            nodeVoList = jobVo.getExecuteJobNodeVoList();
        } else {
            Integer isAll = jobVo.getActionParam().getInteger("isAll");
            if (!Objects.equals(isAll, 1)) {
                currentResourceIdListValid(jobVo);
            } else {
                jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhaseId()));
            }
            //重置节点 (status、startTime、endTime)
            autoexecJobMapper.updateJobPhaseNodeListStatus(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());
            nodeVoList = autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        }
        resetJobNodeStatus(jobVo,nodeVoList);
        return null;
    }
}
