/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobGroupVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

/**
 *
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ReFireAutoexecJobPhaseApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "重跑作业阶段";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "phaseId", type = ApiParamType.LONG, desc = "作业阶段id", isRequired = true),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "refireResetAll,refireAll", desc = "重跑类型：   重置并重跑所有：refireResetAll；重跑所有：refireAll,默认重置并重跑所有")
    })
    @Output({
    })
    @Description(desc = "重跑作业阶段接口")
    @ResubmitInterval(value = 4)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long phaseId = jsonObj.getLong("phaseId");
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(phaseId);
        if(phaseVo==null){
            throw new AutoexecJobPhaseNotFoundException(phaseId.toString());
        }
        AutoexecJobGroupVo jobGroupVo = autoexecJobMapper.getJobGroupById(phaseVo.getGroupId());
        phaseVo.setJobGroupVo(jobGroupVo);
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(phaseVo.getJobId());
        jobVo.setExecuteJobGroupVo(jobGroupVo);
        jobVo.setExecuteJobPhaseList(Collections.singletonList(phaseVo));
        jobVo.setCurrentPhase(phaseVo);
        jobVo.setAction(JobAction.RESET_REFIRE.getValue());
        if(jsonObj.containsKey("type")){
            jobVo.setAction(jsonObj.getString("type"));
        }
        jobVo.setIsNoFireNext(1);
        IAutoexecJobActionHandler refireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.REFIRE_PHASE.getValue());
        return refireAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/refire";
    }
}
