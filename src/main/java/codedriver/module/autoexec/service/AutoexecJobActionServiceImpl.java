/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.AutoexecJobSourceVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.exception.AutoexecCombopCannotExecuteException;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerFactory;
import codedriver.framework.autoexec.job.source.action.IAutoexecJobSourceActionHandler;
import codedriver.framework.autoexec.script.paramtype.IScriptParamType;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import codedriver.framework.autoexec.source.AutoexecJobSourceFactory;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.user.UserNotFoundException;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSON;
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
    AutoexecJobServiceImpl autoexecJobService;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    ResourceCenterMapper resourceCenterMapper;

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
        paramJson.put("roundCount", jobVo.getThreadCount());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        paramJson.put("passThroughEnv", null); //回调需要的返回的参数
        JSONArray paramArray = jobVo.getParamArray();
        JSONObject argJson = new JSONObject() {{
            for (Object paramObj : paramArray) {
                JSONObject paramTmp = JSONObject.parseObject(paramObj.toString());
                put(paramTmp.getString("key"), getValueByParamType(paramTmp));
            }
        }};
        //工具库测试|重跑节点
        if (CollectionUtils.isNotEmpty(jobVo.getExecuteJobNodeVoList())) {
            paramJson.put("noFireNext", 1);
            List<AutoexecJobPhaseNodeVo> nodeVoList = jobVo.getExecuteJobNodeVoList();
            Long protocolId = nodeVoList.get(0).getProtocolId();
            String userName = nodeVoList.get(0).getUserName();
            paramJson.put("runNode", new JSONArray() {{
                Map<Long, AccountVo> resourceAccountMap = new HashMap<>();
                List<AccountVo> accountVoList = resourceCenterMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).collect(Collectors.toList()), protocolId, userName);
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
                    put("phases", new JSONArray() {{
                        for (AutoexecJobPhaseVo jobPhase : groupJobPhaseList) {
                            add(new JSONObject() {{
                                put("phaseName", jobPhase.getName());
                                put("execRound", jobPhase.getExecutePolicy());
                                put("operations", getOperationFireParam(jobPhase.getOperationList()));
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
        IAutoexecJobSourceActionHandler autoexecJobSourceActionHandler = AutoexecJobSourceActionHandlerFactory.getAction(jobSourceVo.getType());
        autoexecJobSourceActionHandler.getFireParamJson(paramJson, jobVo);
    }

    /**
     * 获取作业工具param
     *
     * @param jobOperationVoList 作业工具列表
     * @return 作业工具param
     */
    private JSONArray getOperationFireParam(List<AutoexecJobPhaseOperationVo> jobOperationVoList) {
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
                    put("failIgnore", operationVo.getFailIgnore());
                    put("isScript", Objects.equals(operationVo.getType(), ToolType.SCRIPT.getValue()) ? 1 : 0);
                    put("scriptId", operationVo.getScriptId());
                    put("interpreter", operationVo.getParser());
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
                                    put(argJson.getString("key"), getValueByParamType(argJson));
                                } else if (Objects.equals(ParamMappingMode.RUNTIME_PARAM.getValue(), argJson.getString("mappingMode"))) {
                                    put(argJson.getString("key"), String.format("${%s}", value));
                                } else if (Objects.equals(ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue(), argJson.getString("mappingMode"))) {
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
                            put("if", getOperationFireParam(ifJobOperationList));
                        }
                        JSONArray elseArray = param.getJSONArray("elseList");
                        if (CollectionUtils.isNotEmpty(elseArray)) {
                            List<AutoexecJobPhaseOperationVo> elseJobOperationList = JSONObject.parseArray(elseArray.toJSONString(), AutoexecJobPhaseOperationVo.class);
                            put("else", getOperationFireParam(elseJobOperationList));
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
    private Object getValueByParamType(JSONObject param) {
        String type = param.getString("type");
        Object value = param.get("value");
        if (value != null) {
            IScriptParamType paramType = ScriptParamTypeFactory.getHandler(type);
            if (paramType != null) {
                value = paramType.getAutoexecParamByValue(value);
            }
        }
        return value;
    }

    @Override
    public AutoexecJobVo validateAndCreateJobFromCombop(JSONObject jsonObj, boolean isNeedAuth) {
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jsonObj.getString("source"));
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(jsonObj.getString("source"));
        }
        IAutoexecJobSourceActionHandler autoexecJobSourceActionHandler = AutoexecJobSourceActionHandlerFactory.getAction(jobSourceVo.getType());
        AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getAutoexecCombop(jsonObj);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(jsonObj.getLong("combopId"));
        }
        //作业执行权限校验
        if (isNeedAuth) {
            autoexecCombopService.setOperableButtonList(combopVo);
            if (combopVo.getExecutable() != 1) {
                throw new AutoexecCombopCannotExecuteException(combopVo.getName());
            }
        }
        //设置作业执行节点
        if (combopVo.getConfig() != null && jsonObj.containsKey("executeConfig")) {
            //如果执行传进来的"执行用户"、"协议"为空则使用默认设定的值
            AutoexecCombopExecuteConfigVo executeConfigVo = combopVo.getConfig().getExecuteConfig();
            if (executeConfigVo == null) {
                executeConfigVo = new AutoexecCombopExecuteConfigVo();
            }

            AutoexecCombopExecuteConfigVo paramExecuteConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            if (paramExecuteConfigVo.getProtocolId() != null) {
                executeConfigVo.setProtocolId(paramExecuteConfigVo.getProtocolId());
            }
            if (StringUtils.isNotBlank(paramExecuteConfigVo.getExecuteUser())) {
                executeConfigVo.setExecuteUser(paramExecuteConfigVo.getExecuteUser());
            }
            executeConfigVo.setExecuteNodeConfig(paramExecuteConfigVo.getExecuteNodeConfig());
            combopVo.getConfig().setExecuteConfig(executeConfigVo);

        }
        autoexecCombopService.verifyAutoexecCombopConfig(combopVo, true);
        //根据场景名获取场景id
        if(jsonObj.containsKey("scenarioName")){
            AutoexecScenarioVo scenarioVo = autoexecScenarioMapper.getScenarioByName(jsonObj.getString("scenarioName"));
            if(scenarioVo == null){
                throw new AutoexecScenarioIsNotFoundException(jsonObj.getString("scenarioName"));
            }
            jsonObj.put("scenarioId",scenarioVo.getId());
        }

        Integer threadCount = jsonObj.getInteger("threadCount");
        if(threadCount == null || threadCount < 1){
            throw  new ParamIrregularException("threadCount");
        }

        AutoexecJobVo jobVo = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        jobVo.setConfigStr(combopVo.getConfigStr());
        jobVo.setRunTimeParamList(autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopVo.getId()));
        autoexecJobSourceActionHandler.updateInvokeJob(jsonObj,jobVo);
        autoexecJobService.saveAutoexecCombopJob(combopVo, jobVo);
        jobVo.setAction(JobAction.FIRE.getValue());
        //jobVo.setCurrentGroupSort(0);
        return jobVo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateCreateJob(JSONObject param, boolean isNeedAuth) throws Exception {
        AutoexecJobVo jobVo = validateAndCreateJobFromCombop(param, isNeedAuth);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        fireAction.doService(jobVo);
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
        //初始化执行用户上下文
        UserVo execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        if (execUser == null) {
            throw new UserNotFoundException(jobVo.getExecUser());
        }
        UserContext.init(execUser, "+8:00");
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
    }
}
