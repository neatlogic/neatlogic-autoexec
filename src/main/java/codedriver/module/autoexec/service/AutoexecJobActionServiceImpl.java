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
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.exception.AutoexecCombopCannotExecuteException;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobThreadCountException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.autoexec.script.paramtype.IScriptParamType;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
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
        JSONArray paramArray = jobVo.getParam();
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
                    AutoexecJobGroupVo jobGroupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobVo.getId(),groupSort);
                    put("execStrategy", jobGroupVo.getPolicy());
                    put("phases", new JSONArray() {{
                        for (AutoexecJobPhaseVo jobPhase : groupJobPhaseList) {
                            add(new JSONObject() {{
                                put("phaseName", jobPhase.getName());
                                put("execRound", jobPhase.getExecutePolicy());
                                put("operations", new JSONArray() {{
                                    for (AutoexecJobPhaseOperationVo operationVo : jobPhase.getOperationList()) {
                                        add(new JSONObject() {{
                                            put("opId", operationVo.getName() + "_" + operationVo.getId());
                                            put("opName", operationVo.getName());
                                            put("opType", operationVo.getExecMode());
                                            put("failIgnore", operationVo.getFailIgnore());
                                            put("isScript", Objects.equals(operationVo.getType(), ToolType.SCRIPT.getValue()) ? 1 : 0);
                                            put("scriptId", operationVo.getScriptId());
                                            put("interpreter", operationVo.getParser());
                                            //put("script", operationVo.getScript());
                                            JSONObject param = operationVo.getParam();
                                            put("arg", param.getJSONObject("argument"));
                                            put("opt", new JSONObject() {{
                                                for (Object arg : param.getJSONArray("inputParamList")) {
                                                    JSONObject argJson = JSONObject.parseObject(arg.toString());
                                                    String value = argJson.getString("value");
                                                    if (Objects.equals(ParamMappingMode.CONSTANT.getValue(), argJson.getString("mappingMode"))) {
                                                        put(argJson.getString("key"), getValueByParamType(argJson));
                                                    } else if (Objects.equals(ParamMappingMode.RUNTIME_PARAM.getValue(), argJson.getString("mappingMode"))) {
                                                        put(argJson.getString("key"), String.format("${%s}", value));
                                                    } else if (Objects.equals(ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue(), argJson.getString("mappingMode"))) {
                                                        put(argJson.getString("key"), value);
                                                    } else {
                                                        put(argJson.getString("key"), StringUtils.EMPTY);
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
                                                        put(argJson.getString("key"), argJson.getString("value"));
                                                    }
                                                }
                                            }});
                                        }});
                                    }
                                }});
                            }});
                        }
                    }});
                }});
            }
        }});
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
    public AutoexecJobVo validateCreateJobFromCombop(JSONObject jsonObj, boolean isNeedAuth) {
        Long combopId = jsonObj.getLong("combopId");
        String source = jsonObj.getString("source");
        int threadCount = jsonObj.getInteger("threadCount") == null ? 64 : jsonObj.getInteger("threadCount");
        JSONObject paramJson = jsonObj.getJSONObject("param");
        Date planStartTime = jsonObj.getDate("planStartTime");
        String triggerType = jsonObj.getString("triggerType");
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
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
        //TODO 校验执行参数

        //并发数必须是2的n次方
        if ((threadCount & (threadCount - 1)) != 0) {
            throw new AutoexecJobThreadCountException();
        }
        AutoexecJobInvokeVo invokeVo = new AutoexecJobInvokeVo(jsonObj.getLong("invokeId"), source);
        if (jsonObj.containsKey("name")) {
            combopVo.setName(jsonObj.getString("name"));
        }
        AutoexecJobVo jobVo = autoexecJobService.saveAutoexecCombopJob(combopVo, invokeVo, threadCount, paramJson, planStartTime, triggerType);
        jobVo.setAction(JobAction.FIRE.getValue());
        //jobVo.setCurrentGroupSort(0);
        return jobVo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateCreateJob(JSONObject param, boolean isNeedAuth) throws Exception {
        AutoexecJobVo jobVo = validateCreateJobFromCombop(param, isNeedAuth);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        fireAction.doService(jobVo);
    }

    @Override
    public void getJobDetailAndFireJob(AutoexecJobVo jobVo) throws Exception {
        if (jobVo != null) {
            autoexecJobService.getAutoexecJobDetail(jobVo, 0);
            jobVo.setAction(JobAction.FIRE.getValue());
            jobVo.setCurrentPhaseSort(0);
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            jobVo.setAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }
}
