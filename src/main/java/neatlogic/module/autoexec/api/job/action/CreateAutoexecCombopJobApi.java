/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecCombopJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "组合工具创建作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "combopVersionId", type = ApiParamType.LONG, desc = "组合工具版本ID"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "分组数 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "触发方式")
    })
    @Output({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID")
    })
    @Description(desc = "组合工具创建作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = paramObj.toJavaObject(AutoexecJobVo.class);
        Long combopId = paramObj.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        jobVo.setOperationId(combopId);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        Long combopVersionId = paramObj.getLong("combopVersionId");
        if (combopVersionId != null) {
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(combopVersionId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(combopVersionId);
            }
            jobVo.setCombopVersionId(combopVersionId);
            jobVo.setInvokeId(combopVersionId);
            jobVo.setRouteId(combopVersionId.toString());
            jobVo.setSource(JobSource.COMBOP_TEST.getValue());
        } else {
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(combopId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
            }
            jobVo.setCombopVersionId(autoexecCombopVersionVo.getId());
            jobVo.setInvokeId(autoexecCombopVersionVo.getId());;
            jobVo.setRouteId(autoexecCombopVersionVo.getId().toString());
            jobVo.setSource(JobSource.COMBOP.getValue());
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(jobVo);
        autoexecJobActionService.settingJobFireMode(jobVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", jobVo.getId());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "/autoexec/combop/job/create";
    }
}
