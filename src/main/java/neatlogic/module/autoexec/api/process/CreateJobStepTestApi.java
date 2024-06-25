/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.api.process;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.process.dto.CreateJobConfigConfigVo;
import neatlogic.module.autoexec.process.util.ParseCreateJobConfigUtil;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class CreateJobStepTestApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Override
    public String getName() {
        return "测试新自动化节点";
    }

    @Input({
            @Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单ID"),
            @Param(name = "createJobConfigConfig", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONOBJECT)
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long processTaskId = paramObj.getLong("processTaskId");
        JSONObject createJobConfigConfig = paramObj.getJSONObject("createJobConfigConfig");
        ProcessTaskStepVo processTaskStepVo = new ProcessTaskStepVo();
        processTaskStepVo.setProcessTaskId(processTaskId);
        processTaskStepVo.setId(1L);
        CreateJobConfigConfigVo createJobConfigConfigVo = createJobConfigConfig.toJavaObject(CreateJobConfigConfigVo.class);
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(createJobConfigConfigVo.getCombopId());
        if (activeVersionId == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(createJobConfigConfigVo.getCombopId());
        }
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(activeVersionId);
        }
        List<AutoexecJobVo> autoexecJobList = ParseCreateJobConfigUtil.createAutoexecJobList(processTaskStepVo, createJobConfigConfigVo, autoexecCombopVersionVo);
        JSONArray jobArray = new JSONArray();
        for (AutoexecJobVo jobVo : autoexecJobList) {
            JSONObject jobObj = new JSONObject();
            jobObj.put("param", jobVo.getParam());
            jobObj.put("scenarioId", jobVo.getScenarioId());
            jobObj.put("executeConfig", jobVo.getExecuteConfig());
            jobObj.put("runnerGroup", jobVo.getRunnerGroup());
            jobObj.put("id", jobVo.getId());
            jobObj.put("name", jobVo.getName());
            jobObj.put("source", jobVo.getSource());
            jobObj.put("roundCount", jobVo.getRoundCount());
            jobObj.put("operationId", jobVo.getOperationId());
            jobObj.put("operationType", jobVo.getOperationType());
            jobObj.put("invokeId", jobVo.getInvokeId());
            jobObj.put("routeId", jobVo.getRouteId());
            jobObj.put("isFirstFire", jobVo.getIsFirstFire());
            jobObj.put("assignExecUser", jobVo.getAssignExecUser());
            jobArray.add(jobObj);
        }
        System.out.println("autoexecJobList = " + jobArray.toJSONString());
        JSONObject resultObj = new JSONObject();
        resultObj.put("autoexecJobList", jobArray);
        return resultObj;
    }

    @Override
    public String getToken() {
        return "create/job/step/test";
    }
}
