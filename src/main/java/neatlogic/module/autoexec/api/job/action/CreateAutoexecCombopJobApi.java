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

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecParallelPolicy;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
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

import javax.annotation.Resource;

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
        return "nmaaja.createautoexeccombopjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.combopid"),
            @Param(name = "combopVersionId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.versionid"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.name"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "term.autoexec.executeparam"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.scenarioname"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "nmaaja.createautoexeccombopjobapi.input.param.roundcount"),
            @Param(name = "parallelCount", type = ApiParamType.LONG, desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelcount"),
            @Param(name = "parallelPolicy", type = ApiParamType.ENUM, member = AutoexecParallelPolicy.class,desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelpolicy"),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.triggertype"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "common.runnergroup"),
            @Param(name = "runnerGroupTag", type = ApiParamType.JSONOBJECT, desc = "common.runnergrouptag"),
    })
    @Output({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid")
    })
    @Description(desc = "nmaaja.createautoexeccombopjobapi.getname")
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
            AutoexecCombopVersionConfigVo versionVoConfig = autoexecCombopVersionVo.getConfig();
            if (versionVoConfig != null) {
                AutoexecCombopExecuteConfigVo executeConfig = versionVoConfig.getExecuteConfig();
                if (executeConfig != null) {
                    jobVo.setPreCondition(executeConfig.getPreCondition());
                    jobVo.setWhenToSpecify(executeConfig.getWhenToSpecify());
                }
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
            AutoexecCombopVersionConfigVo versionVoConfig = autoexecCombopVersionVo.getConfig();
            if (versionVoConfig != null) {
                AutoexecCombopExecuteConfigVo executeConfig = versionVoConfig.getExecuteConfig();
                if (executeConfig != null) {
                    jobVo.setPreCondition(executeConfig.getPreCondition());
                    jobVo.setWhenToSpecify(executeConfig.getWhenToSpecify());
                }
            }
            jobVo.setCombopVersionId(autoexecCombopVersionVo.getId());
            jobVo.setInvokeId(autoexecCombopVersionVo.getId());
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
