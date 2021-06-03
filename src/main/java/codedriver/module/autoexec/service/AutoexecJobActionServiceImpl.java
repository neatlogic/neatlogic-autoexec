/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dto.AutoexecRunnerVo;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectAuthException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerConnectRefusedException;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RestVo;
import codedriver.framework.integration.authentication.costvalue.AuthenticateType;
import codedriver.framework.util.RestUtil;
import codedriver.module.autoexec.config.AutoexecConfig;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/27 11:30
 **/
@Service
public class AutoexecJobActionServiceImpl implements AutoexecJobActionService {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobActionServiceImpl.class);

    @Resource
    AutoexecJobAuthActionManager autoexecJobAuthActionManager;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobServiceImpl autoexecJobService;

    @Resource
    UserMapper userMapper;

    /**
     * 第一次执行/重跑/继续作业
     *
     * @param jobVo 作业
     */
    @Override
    public void fire(AutoexecJobVo jobVo) {
        autoexecJobMapper.getJobLockByJobId(jobVo.getId());
        new AutoexecJobAuthActionManager.Builder().addFireJob().addReFireJob().build().setAutoexecJobAction(jobVo);
        if (jobVo.getIsCanJobFire() == 1||jobVo.getIsCanJobReFire() == 1) {
            jobVo.setStatus(JobStatus.RUNNING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
            int sort = 0;
            for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
                jobPhase.setStatus(JobPhaseStatus.WAITING.getValue());
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
                sort = jobPhase.getSort();
            }
            autoexecJobService.refreshJobPhaseNodeList(jobVo.getId(), sort);
            JSONObject paramJson = new JSONObject();
            getFireParamJson(paramJson, jobVo);
            List<AutoexecRunnerVo> runnerVos = autoexecJobMapper.getJobRunnerListByJobId(jobVo.getId());
            List<String> refusedErrorList = new ArrayList<>();
            List<String> authErrorList = new ArrayList<>();
            for (AutoexecRunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/exec";
                RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
                String result = RestUtil.sendRequest(restVo);
                JSONObject resultJson = null;
                try {
                    resultJson = JSONObject.parseObject(result);
                    if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                        authErrorList.add(restVo.getUrl() + ":" + resultJson.getString("Message"));
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    refusedErrorList.add(restVo.getUrl() + " " + result);
                }
            }
            if (CollectionUtils.isNotEmpty(refusedErrorList)) {
                throw new AutoexecJobRunnerConnectRefusedException(String.join(";", refusedErrorList));
            }

            if (CollectionUtils.isNotEmpty(authErrorList)) {
                throw new AutoexecJobRunnerConnectAuthException(String.join(";", authErrorList));
            }

        }
    }

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
        paramJson.put("parallel", jobVo.getThreadCount());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        paramJson.put("passThroughEnv", null); //回调需要的返回的参数
        JSONArray paramArray = jobVo.getParam();
        JSONObject argJson = new JSONObject() {{
            for (Object paramObj : paramArray) {
                JSONObject paramTmp = JSONObject.parseObject(paramObj.toString());
                put(paramTmp.getString("key"), getValueByParamType(paramTmp));
            }
        }};
        paramJson.put("arg", argJson);
        paramJson.put("runFlow", new JSONArray() {{
            for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
                add(new JSONObject() {{
                    put(jobPhase.getName(), new JSONArray() {{
                        for (AutoexecJobPhaseOperationVo operationVo : jobPhase.getOperationList()) {
                            add(new JSONObject() {{
                                put("opId", operationVo.getName() + "_" + operationVo.getId());
                                put("opName", operationVo.getName());
                                put("opType", operationVo.getExecMode());
                                //如果执行方式 是runner 则需要返回runner节点信息
                                /*if(!Objects.equals( operationVo.getExecMode(),ExecMode.TARGET.getValue())){
                                    AutoexecJobPhaseNodeVo jobPhaseNodeVo = autoexecJobMapper.getJobPhaseRunnerNodeByJobIdAndPhaseId(jobVo.getId(),operationVo.getId());
                                    if(jobPhaseNodeVo == null){
                                        throw new AutoexecJobPhaseRunnerNodeNotFoundException(jobVo.getId()+":"+operationVo.getName()+"("+operationVo.getId()+")");
                                    }
                                    put("runnerNode",new JSONObject(){{
                                        put("ip",jobPhaseNodeVo.getHost());
                                        put("port",jobPhaseNodeVo.getPort());
                                    }});
                                }*/
                                put("failIgnore", operationVo.getFailIgnore());
                                put("isScript", Objects.equals(operationVo.getType(), ToolType.SCRIPT.getValue()) ? 1 : 0);
                                put("scriptId", operationVo.getScriptId());
                                put("interpreter", operationVo.getParser());
                                //TODO tool 暂未实现
                                //put("script", operationVo.getScript());
                                JSONObject param = operationVo.getParam();
                                put("arg", new JSONObject() {{
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
                                    for (Object arg : param.getJSONArray("inputParamList")) {
                                        JSONObject argJson = JSONObject.parseObject(arg.toString());
                                        put(argJson.getString("key"), argJson.getString("type"));
                                    }
                                }});
                                put("output", new JSONObject() {{
                                    for (Object arg : param.getJSONArray("outputParamList")) {
                                        JSONObject argJson = JSONObject.parseObject(arg.toString());
                                        put(argJson.getString("key"), argJson.getString("value"));
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
        if (Objects.equals(type, ParamType.FILE.getValue())) {
            value = JSONObject.parseObject(value.toString()).getJSONArray("fileIdList");
        } else if (Objects.equals(type, ParamType.NODE.getValue())) {
            JSONArray nodeJsonArray = JSONObject.parseArray(value.toString());
            for (Object node : nodeJsonArray) {
                JSONObject nodeJson = (JSONObject) node;
                nodeJson.put("ip", nodeJson.getString("host"));
            }
            value = nodeJsonArray;
        }
        return value;
    }

    @Override
    public JSONObject tailNodeLog(JSONObject paramJson) {
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/log/tail";
        RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
        String result = RestUtil.sendRequest(restVo);
        JSONObject resultJson = null;
        try {
            resultJson = JSONObject.parseObject(result);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new AutoexecJobRunnerConnectAuthException(resultJson.getString("Message"));
        } else {
            return resultJson.getJSONObject("Return");
        }
    }

    /**
     * 重跑作业
     *
     * @param jobVo 作业
     */
    @Override
    public void reFire(AutoexecJobVo jobVo) {
    }

    @Override
    public JSONObject tailConsoleLog(JSONObject paramJson) {
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/console/log/tail";
        RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
        String result = RestUtil.sendRequest(restVo);
        JSONObject resultJson = null;
        try {
            resultJson = JSONObject.parseObject(result);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new AutoexecJobRunnerConnectAuthException(resultJson.getString("Message"));
        } else {
            return resultJson.getJSONObject("Return");
        }
    }

    /**
     * 暂停作业
     *
     * @param jobVo 作业
     */
    @Override
    public void pause(AutoexecJobVo jobVo) {

    }

    /**
     * 中止作业
     *
     * @param jobVo 作业
     */
    @Override
    public void abort(AutoexecJobVo jobVo) {
        new AutoexecJobAuthActionManager.Builder().addAbortJob().build().setAutoexecJobAction(jobVo);
        if(jobVo.getIsCanJobAbort() == 1) {
            List<AutoexecRunnerVo> runnerVos = autoexecJobMapper.getJobRunnerListByJobId(jobVo.getId());
            List<String> refusedErrorList = new ArrayList<>();
            List<String> authErrorList = new ArrayList<>();
            JSONObject paramJson = new JSONObject();
            for (AutoexecRunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/abort";
                RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
                String result = RestUtil.sendRequest(restVo);
                JSONObject resultJson = null;
                try {
                    resultJson = JSONObject.parseObject(result);
                    if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                        authErrorList.add(restVo.getUrl() + ":" + resultJson.getString("Message"));
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    refusedErrorList.add(restVo.getUrl() + " " + result);
                }
            }
        }
    }

    /**
     * 重置作业节点
     *
     * @param jobPhaseNode 重置作业节点
     */
    @Override
    public void reset(AutoexecJobPhaseNodeVo jobPhaseNode) {

    }

    /**
     * 忽略作业节点
     *
     * @param jobPhase 作业剧本
     */
    @Override
    public void ignore(AutoexecJobPhaseVo jobPhase) {

    }

    /**
     * 下载作业剧本节点执行情况
     *
     * @param jobPhaseNode 作业剧本节点
     * @param path         日志path
     */
    @Override
    public void logDownload(AutoexecJobPhaseNodeVo jobPhaseNode, String path) {

    }

    /**
     * 获取作业剧本节点执行记录
     *
     * @param paramJson 参数
     * @return 记录列表
     */
    @Override
    public List<AutoexecJobPhaseNodeAuditVo> getNodeAudit(JSONObject paramJson) throws ParseException {
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/execute/audit/get";
        RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
        String result = RestUtil.sendRequest(restVo);
        List<AutoexecJobPhaseNodeAuditVo> auditList = new ArrayList<>();
        JSONObject resultJson = null;
        try {
            resultJson = JSONObject.parseObject(result);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new AutoexecJobRunnerConnectAuthException(resultJson.getString("Message"));
        } else {
            JSONArray auditArray = resultJson.getJSONArray("Return");
            for (Object audit : auditArray) {
                JSONObject auditJson = (JSONObject) audit;
                AutoexecJobPhaseNodeAuditVo auditVo = new AutoexecJobPhaseNodeAuditVo(auditJson);
                auditVo.setExecUserVo(userMapper.getUserBaseInfoByUuidWithoutCache(auditVo.getExecUser()));
                //TODO download
                //auditVo.setDownloadPath(String.format(""));
                auditList.add(auditVo);
            }
        }
        return auditList;
    }

    @Override
    public Object getNodeOperationStatus(JSONObject paramJson) {
        List<AutoexecJobPhaseNodeOperationStatusVo> statusList = new ArrayList<>();
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/status/get";
        RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
        String restResult = RestUtil.sendRequest(restVo);
        JSONObject resultJson = null;
        try {
            resultJson = JSONObject.parseObject(restResult);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + restResult);
        }
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new AutoexecJobRunnerConnectAuthException(resultJson.getString("Message"));
        } else {
            String resultStr = resultJson.getString("Return");
            if (StringUtils.isNotBlank(resultStr)) {
                JSONObject statusJson = JSONObject.parseObject(resultStr);
                List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(paramJson.getLong("jobId"), paramJson.getLong("phaseId"));
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    statusList.add(new AutoexecJobPhaseNodeOperationStatusVo(operationVo, statusJson));
                }
            }
        }
        return statusList.stream().sorted(Comparator.comparing(AutoexecJobPhaseNodeOperationStatusVo::getSort)).collect(Collectors.toList());
    }
}
