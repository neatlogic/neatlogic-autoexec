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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_CREATE_PUBLIC_JOB;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopVersionNotFoundEditTargetException;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecCombopMapper combopMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String getName() {
        return "nmaaja.createautoexecjobfromcomboppublicapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopName", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.combop"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.name"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "term.autoexec.executeparam"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.invokeid"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.parentid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.scenarioname"),
            @Param(name = "parallelPolicy", type = ApiParamType.ENUM, member = AutoexecParallelPolicy.class, desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelpolicy"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "term.autoexec.roundcount"),
            @Param(name = "parallelCount", type = ApiParamType.LONG, desc = "term.autoexec.roundcount"),
//            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "协议名"),
            @Param(name = "protocolId", type = ApiParamType.LONG, desc = "协议id"),
            @Param(name = "executeUser", type = ApiParamType.JSONOBJECT, desc = "执行用户"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标配置"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.triggertype"),
            @Param(name = "assignExecUser", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcomboppublicapi.input.param.assignuser"),
            @Param(name = "runnerGroup", type = ApiParamType.STRING, desc = "nfac.paramtype.runnergroup"),
            @Param(name = "runnerGroupTag", type = ApiParamType.STRING, desc = "nfac.paramtype.runnergrouptag")
    })
    @Output({
    })
    @Description(desc = "nmaaja.createautoexecjobfromcomboppublicapi.description.desc")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String execUserUuid = jsonObj.getString("assignExecUser");
        if (StringUtils.isBlank(execUserUuid)) {
            execUserUuid = UserContext.get().getUserUuid();
        }
        UserVo execUser;
        AuthenticationInfoVo authenticationInfoVo;
        if (Objects.equals(SystemUser.SYSTEM.getUserUuid(), execUserUuid)) {
            execUser = SystemUser.SYSTEM.getUserVo();
            authenticationInfoVo = SystemUser.SYSTEM.getAuthenticationInfoVo();
        } else if (Objects.equals(SystemUser.AUTOEXEC.getUserUuid(), execUserUuid)) {
            //autoexec脚本用的是autoexec虚拟用户
            execUser = SystemUser.AUTOEXEC.getUserVo();
            authenticationInfoVo = SystemUser.AUTOEXEC.getAuthenticationInfoVo();
        } else {
            execUser = userMapper.getUserByUser(execUserUuid);
            if (execUser == null) {
                throw new UserNotFoundException(execUserUuid);
            }
            authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(execUserUuid);
        }
        UserContext.init(execUser, authenticationInfoVo, SystemUser.SYSTEM.getTimezone());
        String combopName = jsonObj.getString("combopName");
        AutoexecCombopVo combopVo = combopMapper.getAutoexecCombopByName(combopName);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopName);
        }
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopVo.getId());
        if (activeVersionId == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(combopName);
        }
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundEditTargetException(activeVersionId);
        }
        AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();

        JSONObject param = jsonObj.getJSONObject("param");
        jsonObj.put("param", initParam(param, versionConfig));
        jsonObj.put("execUser", execUserUuid);
        jsonObj.put("operationType", CombopOperationType.COMBOP.getValue());
        jsonObj.put("source", JobSource.COMBOP.getValue());
        jsonObj.put("operationId", combopVo.getId());
        getExecuteConfig(jsonObj);
        String runnerGroup = jsonObj.getString("runnerGroup");
        String runnerGroupTag = jsonObj.getString("runnerGroupTag");
        jsonObj.remove("runnerGroup");
        jsonObj.remove("runnerGroupTag");
        AutoexecJobVo autoexecJobParam = jsonObj.toJavaObject(AutoexecJobVo.class);
        //runnerGroup
        if (StringUtils.isNotBlank(runnerGroup)) {
            ParamMappingVo runnerGroupMappingVo = new ParamMappingVo();
            runnerGroupMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            runnerGroupMappingVo.setValue(runnerGroup);
            autoexecJobParam.setRunnerGroup(runnerGroupMappingVo);
        }
        //runnerGroupTag
        if (StringUtils.isNotBlank(runnerGroupTag)) {
            ParamMappingVo runnerGroupTagMappingVo = new ParamMappingVo();
            runnerGroupTagMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            if (runnerGroupTag.startsWith("[") && runnerGroupTag.endsWith("]")) {
                runnerGroupTagMappingVo.setValue(runnerGroupTag);
            } else {
                runnerGroupTagMappingVo.setValue(String.format("[%s]", runnerGroupTag));
            }
            autoexecJobParam.setRunnerGroupTag(runnerGroupTagMappingVo);
        }
//        AutoexecCombopExecuteConfigVo executeConfigVo = autoexecJobParam.getExecuteConfig();
//        if (executeConfigVo != null && StringUtils.isNotBlank(executeConfigVo.getProtocol())) {
//            IResourceAccountCrossoverMapper accountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
//            AccountProtocolVo accountProtocolVo = accountCrossoverMapper.getAccountProtocolVoByProtocolName(executeConfigVo.getProtocol());
//            if (accountProtocolVo == null) {
//                throw new ResourceCenterAccountProtocolNotFoundException(executeConfigVo.getProtocol());
//            }
//            executeConfigVo.setProtocolId(accountProtocolVo.getId());
//        }
        JSONObject executeConfigObj = jsonObj.getJSONObject("executeConfig");
        if (MapUtils.isNotEmpty(executeConfigObj)) {
            AutoexecCombopExecuteConfigVo executeConfigVo = executeConfigObj.toJavaObject(AutoexecCombopExecuteConfigVo.class);
            autoexecJobService.handleOldDataExecuteConfig(executeConfigVo, autoexecJobParam);
        }
        if (versionConfig != null) {
            AutoexecCombopExecuteConfigVo executeConfig = versionConfig.getExecuteConfig();
            if (executeConfig != null) {
                autoexecJobParam.setPreCondition(executeConfig.getPreCondition());
                autoexecJobParam.setWhenToSpecify(executeConfig.getWhenToSpecify());
            }
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobParam);
        autoexecJobActionService.settingJobFireMode(autoexecJobParam);
        JSONObject result = new JSONObject();
        result.put("jobId", autoexecJobParam.getId());
        return result;
    }

    /**
     * 转换补充executeConfig结构
     *
     * @param jsonObj 接口如参数
     */
    private void getExecuteConfig(JSONObject jsonObj) {
        if (!jsonObj.containsKey("executeConfig")) {
            JSONObject executeConfig = new JSONObject();
            jsonObj.put("executeConfig", executeConfig);
            executeConfig.put("protocol", jsonObj.getString("protocol"));
            if(StringUtils.isNotBlank(jsonObj.getString("executeUser"))) {
                JSONObject executeUser = new JSONObject();
                executeUser.put("mappingMode", "constant");
                executeUser.put("value", jsonObj.getString("executeUser"));
                executeConfig.put("executeUser", executeUser);
            }
            JSONObject executeNodeConfig = new JSONObject();
            executeNodeConfig.put("inputNodeList", jsonObj.getJSONArray("ipPortList"));
            executeConfig.put("executeNodeConfig", executeNodeConfig);
        }
    }

    /**
     * 初始化作业参数
     *
     * @param param         接口入参
     * @param versionConfig 组合工具版本配置
     */
    private JSONObject initParam(JSONObject param, AutoexecCombopVersionConfigVo versionConfig) {
        JSONObject newParam = new JSONObject();
        List<AutoexecParamVo> paramList = versionConfig.getRuntimeParamList().stream().filter(o -> !Objects.equals(ParamType.FILE.getValue(), o.getType())).collect(Collectors.toList());
        for (AutoexecParamVo paramVo : paramList) {
            Object value;
            IScriptParamType paramType = ScriptParamTypeFactory.getHandler(paramVo.getType());
            if (param.containsKey(paramVo.getKey()) && param.get(paramVo.getKey()) != null) {
                value = param.get(paramVo.getKey());
            } else {
                value = paramVo.getDefaultValue();
            }
            if (paramType != null) {
                value = paramType.getExchangeParamByValue(value);
            }
            newParam.put(paramVo.getKey(), value);
        }
        return newParam;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create/public";
    }
}
