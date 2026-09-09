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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_CREATE_PUBLIC_JOB;
import neatlogic.framework.autoexec.constvalue.AutoexecParallelPolicy;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.job.AutoexecJobSyncParamNotSupportedException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dto.job.AutoexecCombopJobBuildResultVo;
import neatlogic.module.autoexec.dto.job.AutoexecJobSyncResultVo;
import neatlogic.module.autoexec.service.AutoexecCombopJobCreateService;
import neatlogic.module.autoexec.service.AutoexecCombopJobSyncService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2023/9/20 11:20
 **/

@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = AUTOEXEC_CREATE_PUBLIC_JOB.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecCombopJobPublicApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecJobActionService autoexecJobActionService;
    @Resource
    private AutoexecCombopJobCreateService autoexecCombopJobCreateService;
    @Resource
    private AutoexecCombopJobSyncService autoexecCombopJobSyncService;

    @Override
    public String getName() {
        return "nmaaja.createautoexecjobfromcomboppublicapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopName", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.desc.combopname", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.combopname"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.desc.name", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.name"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.jobparam", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.param"),
            @Param(name = "isSync", type = ApiParamType.BOOLEAN, defaultValue = "false", desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.desc.issync", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.issync"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.invokeid", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.invokeid"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.parentid", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.parentid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.scenarioname", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.scenarioname"),
            @Param(name = "ipPortList", type = ApiParamType.JSONARRAY, desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.desc.ipportlist", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.ipportlist"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.desc.protocol", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.protocol"),
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "term.autoexec.executeuser", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.executeuser"),
            @Param(name = "runnerGroup", type = ApiParamType.STRING, desc = "common.runnergroup", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.runnergroup"),
            @Param(name = "runnerGroupTag", type = ApiParamType.STRING, desc = "common.runnergrouptag", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.runnergrouptag"),
            @Param(name = "parallelPolicy", type = ApiParamType.ENUM, member = AutoexecParallelPolicy.class, desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelpolicy", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.parallelpolicy"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "term.autoexec.roundcount", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.roundcount"),
            @Param(name = "parallelCount", type = ApiParamType.LONG, desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelcount", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.parallelcount"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.triggertype", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.triggertype"),
            @Param(name = "assignExecUser", type = ApiParamType.STRING, desc = "term.autoexec.assignexecuser", help = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.help.assignexecuser")
    })
    @Output({
            @Param(explode = AutoexecJobSyncResultVo.class)
    })
    @Description(desc = "nmaaja.createautoexecjobfromcomboppublicapi.description.desc")
    @Example(title = "common.example", example = "{"
            + "\"combopName\":\"deploy\","
            + "\"name\":\"deploy-production\","
            + "\"param\":{\"env\":\"prod\",\"version\":\"1.0.0\"},"
            + "\"isSync\":true,"
            + "\"scenarioName\":\"production\","
            + "\"ipPortList\":[\"192.168.1.10\",\"192.168.1.11:22\",\"192.168.1.12:2222/app\"],"
            + "\"protocol\":\"ssh\","
            + "\"executeUser\":\"root\","
            + "\"runnerGroup\":\"UAT\","
            + "\"runnerGroupTag\":\"A\","
            + "\"parallelPolicy\":\"parallel\","
            + "\"parallelCount\":10"
            + "}")
    @ResubmitInterval(value = 5)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        boolean isSync = jsonObj.getBooleanValue("isSync");
        initDefaultParam(jsonObj);
        if (isSync) {
            validateSyncParam(jsonObj);
            String requestUserUuid = UserContext.get().getUserUuid();
            String execUserUuid = autoexecCombopJobCreateService.initExecUserContext(requestUserUuid);
            return autoexecCombopJobSyncService.createAndWait(jsonObj, execUserUuid);
        }
        String execUserUuid = autoexecCombopJobCreateService.initExecUserContext(jsonObj.getString("assignExecUser"));
        AutoexecCombopJobBuildResultVo buildResult = autoexecCombopJobCreateService.buildJob(jsonObj, execUserUuid);
        AutoexecJobVo autoexecJobParam = buildResult.getJobVo();
        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobParam);
        autoexecJobActionService.settingJobFireMode(autoexecJobParam);
        JSONObject result = new JSONObject();
        result.put("jobId", autoexecJobParam.getId());
        return result;
    }

    /**
     * 两种模式统一使用组合工具名称和空JSON作为作业名称、运行参数缺省值。
     */
    private void initDefaultParam(JSONObject paramObj) {
        paramObj.put("name", StringUtils.defaultIfBlank(paramObj.getString("name"), paramObj.getString("combopName")));
        if (paramObj.getJSONObject("param") == null) {
            paramObj.put("param", new JSONObject());
        }
    }

    /**
     * 同步模式固定当前用户并立即执行，不接受指定发起用户和计划执行参数。
     */
    private void validateSyncParam(JSONObject paramObj) {
        if (StringUtils.isNotBlank(paramObj.getString("assignExecUser"))) {
            throw new AutoexecJobSyncParamNotSupportedException("assignExecUser");
        }
        if (paramObj.containsKey("planStartTime")) {
            throw new AutoexecJobSyncParamNotSupportedException("planStartTime");
        }
        if (paramObj.containsKey("triggerType")) {
            throw new AutoexecJobSyncParamNotSupportedException("triggerType");
        }
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create/public";
    }
}
