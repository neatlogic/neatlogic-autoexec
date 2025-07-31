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
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
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
@AuthAction(action = AUTOEXEC_CREATE_PUBLIC_JOB.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecCombopJobPublicApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

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

    @Resource
    private RunnerMapper runnerMapper;

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
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
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

        String assignExecUser = UserContext.get().getUserUuid();
        String assignExecUserParam = jsonObj.getString("assignExecUser");
        if (StringUtils.isNotBlank(assignExecUserParam)) {
            UserVo assignUserTmp = userMapper.getUserByUser(assignExecUserParam);
            if (assignUserTmp != null) {
                assignExecUser = assignUserTmp.getUuid();
                AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(assignExecUser);
                UserContext.init(assignUserTmp, authenticationInfo, SystemUser.SYSTEM.getTimezone());
            } else {
                throw new UserNotFoundException(assignExecUserParam);
            }
        }
        JSONObject param = jsonObj.getJSONObject("param");
        jsonObj.put("param", initParam(param, versionConfig));
        jsonObj.put("assignExecUser", assignExecUser);
        jsonObj.put("operationType", CombopOperationType.COMBOP.getValue());
        jsonObj.put("source", JobSource.COMBOP.getValue());
        jsonObj.put("operationId", combopVo.getId());
        getExecuteConfig(jsonObj);
        AutoexecJobVo autoexecJobParam = JSON.toJavaObject(jsonObj, AutoexecJobVo.class);
        //runnerGroup
        String runnerGroup = jsonObj.getString("runnerGroup");
        if (StringUtils.isNotBlank(runnerGroup)) {
            ParamMappingVo runnerGroupMappingVo = new ParamMappingVo();
            runnerGroupMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            runnerGroupMappingVo.setValue(runnerGroup);
            autoexecJobParam.setRunnerGroup(runnerGroupMappingVo);
        }
        //runnerGroupTag
        String runnerGroupTag = jsonObj.getString("runnerGroupTag");
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
        AutoexecCombopExecuteConfigVo executeConfigVo = autoexecJobParam.getExecuteConfig();
        if (executeConfigVo != null && StringUtils.isNotBlank(executeConfigVo.getProtocol())) {
            IResourceAccountCrossoverMapper accountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountProtocolVo accountProtocolVo = accountCrossoverMapper.getAccountProtocolVoByProtocolName(executeConfigVo.getProtocol());
            if (accountProtocolVo == null) {
                throw new ResourceCenterAccountProtocolNotFoundException(executeConfigVo.getProtocol());
            }
            executeConfigVo.setProtocolId(accountProtocolVo.getId());
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
            JSONObject executeUser = new JSONObject();
            executeUser.put("mappingMode", "constant");
            executeUser.put("value", jsonObj.getString("executeUser"));
            executeConfig.put("executeUser", executeUser);
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
