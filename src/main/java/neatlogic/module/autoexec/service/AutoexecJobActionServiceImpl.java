/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.AutoexecJobSourceVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.dto.job.*;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/27 11:30
 **/
@Service
public class AutoexecJobActionServiceImpl implements AutoexecJobActionService, IAutoexecJobActionCrossoverService {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobActionServiceImpl.class);

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    AutoexecProfileServiceImpl autoexecProfileService;

    @Resource
    AutoexecGlobalParamMapper globalParamMapper;

    @Resource
    AutoexecScenarioMapper autoexecScenarioMapper;

    /**
     * 拼装给proxy的param
     *
     * @param paramJson 返回param值
     * @param jobVo     作业
     */
    @Override
    public void getFireParamJson(JSONObject paramJson, AutoexecJobVo jobVo) {
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("preJobId", null); //给后续ITSM对接使用
        paramJson.put("roundCount", jobVo.getRoundCount());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        paramJson.put("passThroughEnv", null); //回调需要的返回的参数
        List<AutoexecParamVo> runTimeParamList = jobVo.getRunTimeParamList();
        JSONObject argJson = new JSONObject();
        for (AutoexecParamVo runtimeParam : runTimeParamList) {
            argJson.put(runtimeParam.getKey(), getValueByParamType(runtimeParam));
        }
        //工具库测试|重跑节点
        if (CollectionUtils.isNotEmpty(jobVo.getExecuteJobNodeVoList())) {
            paramJson.put("noFireNext", 1);
            List<AutoexecJobPhaseNodeVo> nodeVoList = jobVo.getExecuteJobNodeVoList();
            Long protocolId = nodeVoList.get(0).getProtocolId();
            String userName = nodeVoList.get(0).getUserName();
            paramJson.put("runNode", new JSONArray() {{
                Map<Long, AccountVo> resourceAccountMap = new HashMap<>();
                IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
                List<AccountVo> accountVoList = resourceAccountCrossoverMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).collect(Collectors.toList()), protocolId, userName);
                accountVoList.forEach(o -> {
                    resourceAccountMap.put(o.getResourceId(), o);
                });
                for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
                    add(new JSONObject() {{
                        AccountVo accountVo = resourceAccountMap.get(nodeVo.getResourceId());
                        put("protocol", accountVo.getProtocol());
                        put("username", accountVo.getAccount());
                        put("password", accountVo.getPasswordPlain());
                        put("protocolPort", accountVo.getProtocolPort());
                        put("nodeId", nodeVo.getId());
                        put("nodeName", nodeVo.getNodeName());
                        put("nodeType", nodeVo.getNodeType());
                        put("resourceId", nodeVo.getResourceId());
                        put("host", nodeVo.getHost());
                        put("port", nodeVo.getPort());
                    }});
                }
            }});
        }
        paramJson.put("arg", new JSONObject());
        paramJson.put("opt", argJson);
        Map<Integer, List<AutoexecJobPhaseVo>> groupPhaseListMap = new LinkedHashMap<>();
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            groupPhaseListMap.computeIfAbsent(jobPhase.getJobGroupVo().getSort(), k -> new ArrayList<>()).add(jobPhase);
        }
        paramJson.put("runFlow", new JSONArray() {{
            for (Map.Entry<Integer, List<AutoexecJobPhaseVo>> jobPhaseMapEntry : groupPhaseListMap.entrySet()) {
                Integer groupSort = jobPhaseMapEntry.getKey();
                List<AutoexecJobPhaseVo> groupJobPhaseList = jobPhaseMapEntry.getValue();
                add(new JSONObject() {{
                    put("groupNo", groupSort);
                    AutoexecJobGroupVo jobGroupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobVo.getId(), groupSort);
                    put("execStrategy", jobGroupVo.getPolicy());
                    put("roundCount", jobGroupVo.getRoundCount());
                    put("phases", new JSONArray() {{
                        for (AutoexecJobPhaseVo jobPhase : groupJobPhaseList) {
                            add(new JSONObject() {{
                                put("phaseName", jobPhase.getName());
                                put("phaseType", jobPhase.getExecMode());
                                put("execRound", jobPhase.getExecutePolicy());
                                put("roundCount", jobPhase.getRoundCount());
                                put("operations", getOperationFireParam(jobPhase, jobPhase.getOperationList()));
                            }});
                        }
                    }});
                }});
            }
        }});
        //补充各个作业来源类型的特殊参数，如：发布的environment
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
        autoexecJobSourceActionHandler.getFireParamJson(paramJson, jobVo);
    }

    /**
     * 获取作业工具param
     *
     * @param jobOperationVoList 作业工具列表
     * @return 作业工具param
     */
    private JSONArray getOperationFireParam(AutoexecJobPhaseVo jobPhaseVo, List<AutoexecJobPhaseOperationVo> jobOperationVoList) {
        return new JSONArray() {{
            for (AutoexecJobPhaseOperationVo operationVo : jobOperationVoList) {
                JSONObject param = operationVo.getParam();
                JSONArray inputParamArray = param.getJSONArray("inputParamList");
                JSONArray argumentList = param.getJSONArray("argumentList");
                Map<String, Object> profileKeyValueMap = new HashMap<>();
                Map<String, Object> globalParamKeyValueMap = new HashMap<>();
                List<String> globalParamKeyList = new ArrayList<>();
                //批量查询 inputParam profile 和 全局参数的值
                if (CollectionUtils.isNotEmpty(inputParamArray)) {
                    List<String> profileKeyList = new ArrayList<>();
                    for (int i = 0; i < inputParamArray.size(); i++) {
                        JSONObject inputParam = inputParamArray.getJSONObject(i);
                        if (Objects.equals(ParamMappingMode.PROFILE.getValue(), inputParam.getString("mappingMode"))) {
                            profileKeyList.add(inputParam.getString("key"));
                        }
                        if (Objects.equals(ParamMappingMode.GLOBAL_PARAM.getValue(), inputParam.getString("mappingMode"))) {
                            globalParamKeyList.add(inputParam.getString("value"));
                        }
                    }
                    if (operationVo.getProfileId() != null) {
                        profileKeyValueMap = autoexecProfileService.getAutoexecProfileParamListByKeyListAndProfileId(profileKeyList, operationVo.getProfileId());
                    }
                }
                //批量查询 自由参数的全局参数
                if (CollectionUtils.isNotEmpty(argumentList)) {
                    for (int i = 0; i < argumentList.size(); i++) {
                        JSONObject argumentJson = argumentList.getJSONObject(i);
                        if (Objects.equals(ParamMappingMode.GLOBAL_PARAM.getValue(), argumentJson.getString("mappingMode"))) {
                            globalParamKeyList.add(argumentJson.getString("value"));
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(globalParamKeyList)) {
                    List<AutoexecGlobalParamVo> globalParamVos = globalParamMapper.getGlobalParamByKeyList(globalParamKeyList);
                    if (CollectionUtils.isNotEmpty(globalParamVos)) {
                        globalParamKeyValueMap = globalParamVos.stream().filter(o -> o.getDefaultValue() != null).collect(Collectors.toMap(AutoexecGlobalParamVo::getKey, AutoexecGlobalParamVo::getDefaultValue));
                    }

                }
                Map<String, Object> finalProfileKeyValueMap = profileKeyValueMap;
                Map<String, Object> finalGlobalParamKeyValueMap = globalParamKeyValueMap;
                add(new JSONObject() {{
                    put("opId", operationVo.getName() + "_" + operationVo.getId());
                    put("opName", operationVo.getName());
                    put("opType", operationVo.getExecMode());
                    put("opLetter", operationVo.getLetter());
                    put("failIgnore", operationVo.getFailIgnore());
                    put("isScript", Objects.equals(operationVo.getType(), ToolType.SCRIPT.getValue()) ? 1 : 0);
                    put("scriptId", operationVo.getScriptId() == null ? operationVo.getOperationId() : operationVo.getScriptId());
                    put("interpreter", operationVo.getParser());
                    put("help", operationVo.getDescription());
                    //put("script", operationVo.getScript());
                    if (CollectionUtils.isNotEmpty(argumentList)) {
                        for (int i = 0; i < argumentList.size(); i++) {
                            JSONObject argumentJson = argumentList.getJSONObject(i);
                            argumentJson.remove("name");
                            argumentJson.remove("description");
                            if (Objects.equals(ParamMappingMode.RUNTIME_PARAM.getValue(), argumentJson.getString("mappingMode"))) {
                                argumentJson.put("value", String.format("${%s}", argumentJson.getString("value")));
                            }
                            if (Objects.equals(ParamMappingMode.GLOBAL_PARAM.getValue(), argumentJson.getString("mappingMode"))) {
                                argumentJson.put("value", finalGlobalParamKeyValueMap.get(argumentJson.getString("value")));
                            }
                            argumentJson.remove("mappingMode");
                        }
                    }
                    put("arg", argumentList);
                    put("opt", new JSONObject() {{
                        if (CollectionUtils.isNotEmpty(inputParamArray)) {
                            for (Object arg : inputParamArray) {
                                JSONObject argJson = JSONObject.parseObject(arg.toString());
                                String value = argJson.getString("value");
                                if (Objects.equals(ParamMappingMode.CONSTANT.getValue(), argJson.getString("mappingMode"))) {
                                    put(argJson.getString("key"), getValueByParamType(argJson, jobPhaseVo, operationVo));
                                } else if (Objects.equals(ParamMappingMode.RUNTIME_PARAM.getValue(), argJson.getString("mappingMode"))) {
                                    put(argJson.getString("key"), String.format("${%s}", value));
                                } else if (Arrays.asList(ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue(), ParamMappingMode.PRE_NODE_OUTPUT_PARAM_KEY.getValue()).contains(argJson.getString("mappingMode"))) {
                                    put(argJson.getString("key"), value);
                                } else if (Objects.equals(ParamMappingMode.PROFILE.getValue(), argJson.getString("mappingMode"))) {
                                    put(argJson.getString("key"), finalProfileKeyValueMap.get(argJson.getString("key")));
                                } else if (Objects.equals(ParamMappingMode.GLOBAL_PARAM.getValue(), argJson.getString("mappingMode"))) {
                                    put(argJson.getString("key"), finalGlobalParamKeyValueMap.get(argJson.getString("value")));
                                } else {
                                    put(argJson.getString("key"), StringUtils.EMPTY);
                                }
                            }
                        }
                    }});
                    put("desc", new JSONObject() {{
                        if (CollectionUtils.isNotEmpty(param.getJSONArray("inputParamList"))) {
                            for (Object arg : param.getJSONArray("inputParamList")) {
                                JSONObject argJson = JSONObject.parseObject(arg.toString());
                                put(argJson.getString("key"), argJson.getString("type"));
                            }
                        }
                    }});
                    put("output", new JSONObject() {{
                        if (CollectionUtils.isNotEmpty(param.getJSONArray("outputParamList"))) {
                            for (Object arg : param.getJSONArray("outputParamList")) {
                                JSONObject argJson = JSONObject.parseObject(arg.toString());
                                JSONObject outputParamJson = new JSONObject();
                                put(argJson.getString("key"), outputParamJson);
                                outputParamJson.put("opt", argJson.getString("key"));
                                outputParamJson.put("type", argJson.getString("type"));
                                outputParamJson.put("defaultValue", argJson.getString("defaultValue"));
                            }
                        }
                    }});
                    if (StringUtils.isNotBlank(param.getString("condition"))) {
                        put("condition", param.getString("condition"));
                        JSONArray ifArray = param.getJSONArray("ifList");
                        if (CollectionUtils.isNotEmpty(ifArray)) {
                            List<AutoexecJobPhaseOperationVo> ifJobOperationList = JSONObject.parseArray(ifArray.toJSONString(), AutoexecJobPhaseOperationVo.class);
                            put("if", getOperationFireParam(jobPhaseVo, ifJobOperationList));
                        }
                        JSONArray elseArray = param.getJSONArray("elseList");
                        if (CollectionUtils.isNotEmpty(elseArray)) {
                            List<AutoexecJobPhaseOperationVo> elseJobOperationList = JSONObject.parseArray(elseArray.toJSONString(), AutoexecJobPhaseOperationVo.class);
                            put("else", getOperationFireParam(jobPhaseVo, elseJobOperationList));
                        }
                    }
                }});
            }
        }};
    }

    /**
     * 根据参数值类型获取对应参数的值
     *
     * @param param 参数json
     * @return 值
     */
    private Object getValueByParamType(JSONObject param, AutoexecJobPhaseVo jobPhaseVo, AutoexecJobPhaseOperationVo operationVo) {
        String type = param.getString("type");
        Object value = param.get("value");
        try {
            if (value != null) {
                IScriptParamType paramType = ScriptParamTypeFactory.getHandler(type);
                if (paramType != null) {
                    value = paramType.getAutoexecParamByValue(value);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AutoexecJobOperationParamValueInvalidException(jobPhaseVo.getName(), operationVo.getName(), param.getString("name"),param.getString("value"));
        }
        return value;
    }

    /**
     * 根据参数值类型获取对应参数的值
     *
     * @param runtimeParam 参数json
     * @return 值
     */
    private Object getValueByParamType(AutoexecParamVo runtimeParam) {
        String type = runtimeParam.getType();
        Object value = runtimeParam.getValue();
        try {
            if (value != null) {
                IScriptParamType paramType = ScriptParamTypeFactory.getHandler(type);
                if (paramType != null) {
                    value = paramType.getAutoexecParamByValue(value);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AutoexecJobParamValueInvalidException(runtimeParam.getKey(),runtimeParam.getValue());
        }
        return value;
    }

    @Override
    public void validateAndCreateJobFromCombop(AutoexecJobVo autoexecJobParam) {
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(autoexecJobParam.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(autoexecJobParam.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
        AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getAutoexecCombop(autoexecJobParam);
        //作业执行权限校验
        autoexecJobSourceActionHandler.executeAuthCheck(autoexecJobParam, false);
        //设置作业执行节点
        AutoexecCombopConfigVo config = combopVo.getConfig();
        if (config == null) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        if (CollectionUtils.isEmpty(config.getCombopPhaseList())) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        if (CollectionUtils.isEmpty(config.getCombopGroupList())) {
            throw new AutoexecCombopAtLeastOneGroupException();
        }
        if (autoexecJobParam.getExecuteConfig() != null) {
            //如果执行传进来的"执行用户"、"协议"为空则使用默认设定的值
            AutoexecCombopExecuteConfigVo combopExecuteConfigVo = config.getExecuteConfig();
            if (combopExecuteConfigVo == null) {
                combopExecuteConfigVo = new AutoexecCombopExecuteConfigVo();
            }

            if (autoexecJobParam.getExecuteConfig().getProtocolId() != null) {
                combopExecuteConfigVo.setProtocolId(autoexecJobParam.getExecuteConfig().getProtocolId());
            }
            if (StringUtils.isNotBlank(autoexecJobParam.getExecuteConfig().getExecuteUser())) {
                combopExecuteConfigVo.setExecuteUser(autoexecJobParam.getExecuteConfig().getExecuteUser());
            }
            if (autoexecJobParam.getExecuteConfig().getExecuteNodeConfig() != null && !autoexecJobParam.getExecuteConfig().getExecuteNodeConfig().isNull()) {
                combopExecuteConfigVo.setExecuteNodeConfig(autoexecJobParam.getExecuteConfig().getExecuteNodeConfig());
            }
            config.setExecuteConfig(combopExecuteConfigVo);
            autoexecCombopService.verifyAutoexecCombopConfig(config, true);
        }


        //根据场景名获取场景id
        if (StringUtils.isNotBlank(autoexecJobParam.getScenarioName())) {
            AutoexecScenarioVo scenarioVo = autoexecScenarioMapper.getScenarioByName(autoexecJobParam.getScenarioName());
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(autoexecJobParam.getScenarioName());
            }
            autoexecJobParam.setScenarioId(scenarioVo.getId());
        }
        autoexecJobParam.setConfigStr(JSONObject.toJSONString(config));
        autoexecJobParam.setRunTimeParamList(config.getRuntimeParamList());

        autoexecJobSourceActionHandler.updateInvokeJob(autoexecJobParam);
        autoexecJobService.saveAutoexecCombopJob(autoexecJobParam);
        autoexecJobParam.setAction(JobAction.FIRE.getValue());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateCreateJob(AutoexecJobVo jobParam) throws Exception {
        validateAndCreateJobFromCombop(jobParam);
        jobParam.setAction(JobAction.FIRE.getValue());
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        fireAction.doService(jobParam);
    }

    @Override
    public void getJobDetailAndFireJob(AutoexecJobVo jobVo) throws Exception {
        if (jobVo != null) {
            jobVo.setAction(JobAction.FIRE.getValue());
            jobVo.setExecuteJobGroupVo(autoexecJobMapper.getJobGroupByJobIdAndSort(jobVo.getId(), 0));
            autoexecJobService.getAutoexecJobDetail(jobVo);
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            jobVo.setIsFirstFire(1);
            fireAction.doService(jobVo);
        }
    }

    @Override
    public void initExecuteUserContext(AutoexecJobVo jobVo) throws Exception {
        UserVo execUser;
        //初始化执行用户上下文
        if (Objects.equals(jobVo.getExecUser(), SystemUser.SYSTEM.getUserUuid())) {
            execUser = SystemUser.SYSTEM.getUserVo();
        } else {
            execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        }
        if (execUser == null) {
            throw new UserNotFoundException(jobVo.getExecUser());
        }

        UserContext.init(execUser, "+8:00");
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
    }
}
