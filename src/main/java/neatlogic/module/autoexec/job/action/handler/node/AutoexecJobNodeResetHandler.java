/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
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
        Integer isAll = jobVo.getActionParam().getInteger("isAll");
        //更新状态
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getExecutePhase();
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            jobVo.getActionParam().put("phaseName", currentPhaseVo.getName());
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler handler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            handler.resetSqlStatus(jobVo.getActionParam(), jobVo);
        } else {
            if (!Objects.equals(isAll, 1)) {
                currentResourceIdListValid(jobVo);
                //重置节点 (status、startTime、endTime)
                autoexecJobMapper.updateJobPhaseNodeListStatus(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());
                jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList())));
            }
        }
        if (Objects.equals(isAll, 1) || Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            //重置所有或者阶段原来状态已完成的 更新阶段状态为待运行
            autoexecJobMapper.updateJobPhaseStatusByPhaseIdList(Collections.singletonList(currentPhaseVo.getId()), JobPhaseStatus.PENDING.getValue());
            autoexecJobMapper.updateJobPhaseRunnerStatusByJobIdAndPhaseId(jobVo.getId(), currentPhaseVo.getId(), JobPhaseStatus.PENDING.getValue());
            autoexecJobMapper.updateJobPhaseNodeStatusByJobPhaseIdAndIsDelete(currentPhaseVo.getId(), JobNodeStatus.PENDING.getValue(), 0);
        }

        autoexecJobService.resetRunnerJobNodeStatus(jobVo);
        return null;
    }
}
