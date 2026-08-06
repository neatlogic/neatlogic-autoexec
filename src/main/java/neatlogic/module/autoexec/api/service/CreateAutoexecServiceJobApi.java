/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecServiceConfigExpiredException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import neatlogic.module.autoexec.process.dto.AutoexecJobBuilder;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import neatlogic.module.autoexec.service.AutoexecServiceService;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecServiceJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecServiceMapper autoexecServiceMapper;

    @Resource
    private AutoexecServiceService autoexecServiceService;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "nmaas.createautoexecservicejobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "serviceId", type = ApiParamType.LONG, isRequired = true, desc = "common.serviceid"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.name"),
            @Param(name = "formAttributeDataList", type = ApiParamType.JSONARRAY, desc = "term.itsm.formattributedatalist"),
            @Param(name = "hidecomponentList", type = ApiParamType.JSONARRAY, desc = "term.itsm.hidecomponentlist"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "roundCount", type = ApiParamType.INTEGER, desc = "term.autoexec.roundcount"),
            @Param(name = "protocol", type = ApiParamType.LONG, desc = "term.cmdb.protocol"),
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "term.autoexec.executeuser"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
            @Param(name = "runtimeParamMap", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.jobparamlist"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "common.runnergroup"),
            @Param(name = "runnerGroupTag", type = ApiParamType.JSONOBJECT, desc = "common.runnergrouptag"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.triggertype")
    })
    @Output({
    })
    @Description(desc = "nmaas.createautoexecservicejobapi.getname")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long serviceId = paramObj.getLong("serviceId");
        AutoexecServiceVo autoexecServiceVo = autoexecServiceMapper.getAutoexecServiceById(serviceId);
        if (autoexecServiceVo == null) {
            throw new AutoexecServiceNotFoundException(serviceId);
        }
        if (Objects.equals(autoexecServiceVo.getConfigExpired(), 1)) {
            throw new AutoexecServiceConfigExpiredException(autoexecServiceVo.getName());
        }
        List<Long> upwardIdList = autoexecServiceMapper.getUpwardIdListByLftAndRht(autoexecServiceVo.getLft(), autoexecServiceVo.getRht());
        AutoexecServiceSearchVo searchVo = new AutoexecServiceSearchVo();
        searchVo.setServiceIdList(upwardIdList);
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        searchVo.setAuthenticationInfoVo(authenticationInfoVo);
        int count = autoexecServiceMapper.getAllVisibleCount(searchVo);
        if (count < upwardIdList.size()) {
            throw new PermissionDeniedException();
        }
        Long combopId = autoexecServiceVo.getCombopId();
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(combopId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(combopId);
        }
        String name = paramObj.getString("name");
        Long scenarioId = paramObj.getLong("scenarioId");
        JSONArray formAttributeDataList = paramObj.getJSONArray("formAttributeDataList");
        JSONArray hidecomponentList = paramObj.getJSONArray("hidecomponentList");
        Integer roundCount = paramObj.getInteger("roundCount");
        Integer parallelCount = paramObj.getInteger("parallelCount");
        String parallelPolicy = paramObj.getString("parallelPolicy");
        String executeUser = paramObj.getString("executeUser");
        Long protocol = paramObj.getLong("protocol");
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = paramObj.getObject("executeNodeConfig", AutoexecCombopExecuteNodeConfigVo.class);
        JSONObject runtimeParamMap = paramObj.getJSONObject("runtimeParamMap");
        ParamMappingVo runnerGroup = null;
        JSONObject runnerGroupObj = paramObj.getJSONObject("runnerGroup");
        if (MapUtils.isNotEmpty(runnerGroupObj)) {
            runnerGroup = runnerGroupObj.toJavaObject(ParamMappingVo.class);
        }
        ParamMappingVo runnerGroupTag = null;
        JSONObject runnerGroupTagObj = paramObj.getJSONObject("runnerGroupTag");
        if (MapUtils.isNotEmpty(runnerGroupTagObj)) {
            runnerGroupTag = runnerGroupTagObj.toJavaObject(ParamMappingVo.class);
        }

        AutoexecJobBuilder autoexecJobBuilder = autoexecServiceService.getAutoexecJobBuilder(autoexecServiceVo, autoexecCombopVersionVo, name, scenarioId, formAttributeDataList, hidecomponentList, roundCount, parallelCount, parallelPolicy, executeUser, protocol, executeNodeConfig, runtimeParamMap, runnerGroup, runnerGroupTag);
        AutoexecJobVo autoexecJobVo = autoexecJobBuilder.build();
        autoexecJobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        autoexecJobVo.setInvokeId(autoexecServiceVo.getId());
        autoexecJobVo.setRouteId(autoexecServiceVo.getId().toString());
        autoexecJobVo.setSource(JobSource.SERVICE.getValue());
        String triggerType = paramObj.getString("triggerType");
        Long planStartTime = paramObj.getLong("planStartTime");
        autoexecJobVo.setTriggerType(triggerType);
        if (planStartTime != null) {
            autoexecJobVo.setPlanStartTime(new Date(planStartTime));
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobVo);
        autoexecJobActionService.settingJobFireMode(autoexecJobVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", autoexecJobVo.getId());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "autoexec/service/job/create";
    }
}
