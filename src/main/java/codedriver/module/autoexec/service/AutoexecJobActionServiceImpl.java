/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dto.AutoexecRunnerVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RestVo;
import codedriver.framework.integration.authentication.costvalue.AuthenticateType;
import codedriver.framework.util.RestUtil;
import codedriver.module.autoexec.config.AutoexecConfig;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/27 11:30
 **/
@Service
public class AutoexecJobActionServiceImpl implements AutoexecJobActionService {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobActionServiceImpl.class);

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobServiceImpl autoexecJobService;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    UserMapper userMapper;


    @Override
    public void executeAuthCheck(AutoexecJobVo jobVo) {
        if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            if (combopVo == null) {
                throw new AutoexecCombopNotFoundException(jobVo.getOperationId());
            }
            autoexecCombopService.setOperableButtonList(combopVo);
            if (combopVo.getExecutable() != 1) {
                throw new AutoexecCombopCannotExecuteException(combopVo.getName());
            }
        }
    }

    /**
     * 检查runner联通性
     *
     * @return runners
     */
    private List<AutoexecRunnerVo> checkRunnerHealth(AutoexecJobVo jobVo) {
        List<AutoexecRunnerVo> runnerVos = null;
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        try {
            runnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobVo.getId(), jobVo.getPhaseList().stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()));
            for (AutoexecRunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/health/check";
                restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), new JSONObject());
                result = RestUtil.sendRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            assert restVo != null;
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
        }
        return runnerVos;
    }

    /**
     * 执行作业阶段
     *
     * @param jobVo 作业
     */
    private void execute(AutoexecJobVo jobVo) {
        autoexecJobMapper.getJobLockByJobId(jobVo.getId());
        if (jobVo.getIsCanJobFire() == 1 || jobVo.getIsCanJobReFire() == 1) {
            jobVo.setStatus(JobStatus.RUNNING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
            Integer phaseSort = 0;
            for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
                phaseSort = jobPhase.getSort();
                jobPhase.setStatus(JobPhaseStatus.WAITING.getValue());
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
            }
            autoexecJobService.refreshJobPhaseNodeList(jobVo.getId(), jobVo.getCurrentPhaseSort());
            JSONObject paramJson = new JSONObject();
            getFireParamJson(paramJson, jobVo);
            paramJson.put("isFirstFire", jobVo.getCurrentPhaseSort() == 0 ? 1 : 0);

            Integer finalPhaseSort = phaseSort;
            RestVo restVo = null;
            String result = StringUtils.EMPTY;
            try {
                List<AutoexecRunnerVo> runnerVos = checkRunnerHealth(jobVo);
                for (AutoexecRunnerVo runner : runnerVos) {
                    String url = runner.getUrl() + "api/rest/job/exec";
                    paramJson.put("passThroughEnv", new JSONObject() {{
                        put("runnerId", runner.getId());
                        put("phaseSort", finalPhaseSort);
                    }});
                    restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
                    result = RestUtil.sendRequest(restVo);
                    JSONObject resultJson = JSONObject.parseObject(result);
                    if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                        throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                    }
                }
            } catch (Exception ex) {
                assert restVo != null;
                throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
            }
        }
    }

    /**
     * 第一次执行/重跑/继续作业
     *
     * @param jobVo 作业
     */
    @Override
    public void fire(AutoexecJobVo jobVo) {
        new AutoexecJobAuthActionManager.Builder().addFireJob().build().setAutoexecJobAction(jobVo);
        execute(jobVo);
    }

    @Override
    public void refire(AutoexecJobVo jobVo) {
        autoexecJobMapper.updateJobPhaseStatusByJobId(jobVo.getId(), JobPhaseStatus.PENDING.getValue());//重置phase状态为pending
        autoexecJobMapper.updateJobPhaseFailedNodeStatusByJobId(jobVo.getId(), JobNodeStatus.PENDING.getValue());//重置失败的节点的状态为pending
        jobVo.setCurrentPhaseSort(0);//重头开始跑
        new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
        execute(jobVo);
    }

    @Override
    public void goon(AutoexecJobVo jobVo) {
        int sort = 0;
        /*寻找中止|暂停的phase
         * 1、优先寻找aborted|paused phase
         * 2、没有满足1条件的，再寻找pending 最小sort phase
         */
        List<AutoexecJobPhaseVo> autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobVo.getId(), Arrays.asList(JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue()));
        if (CollectionUtils.isNotEmpty(autoexecJobPhaseVos)) {
            sort = autoexecJobPhaseVos.get(0).getSort();
        } else {
            AutoexecJobPhaseVo phase = autoexecJobMapper.getJobPhaseByJobIdAndPhaseStatus(jobVo.getId(), JobPhaseStatus.PENDING.getValue());
            if (phase != null) {
                sort = phase.getSort();
            }
        }
        jobVo.setCurrentPhaseSort(sort);
        autoexecJobService.getAutoexecJobDetail(jobVo, sort);
        new AutoexecJobAuthActionManager.Builder().addFireJob().build().setAutoexecJobAction(jobVo);
        execute(jobVo);
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

    @Override
    public void downloadNodeLog(JSONObject paramJson, HttpServletResponse response) throws IOException {
        String runnerUrl = paramJson.getString("runnerUrl");
        Long jobId = paramJson.getLong("jobId");
        String phase = paramJson.getString("phase");
        String ip = paramJson.getString("ip");
        Integer port = paramJson.getInteger("port");
        String execMode = paramJson.getString("execMode");
        String url = String.format("%s/api/binary/job/phase/node/log/get?jobId=%s&phase=%s&ip=%s&port=%s&execMode=%s", runnerUrl, jobId, phase, ip, port, execMode);
        RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD());
        RestUtil.sendGetRequest(restVo, response);
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
        new AutoexecJobAuthActionManager.Builder().addPauseJob().build().setAutoexecJobAction(jobVo);
        if (jobVo.getIsCanJobPause() == 1) {
            abortOrPauseService(jobVo, JobStatus.PAUSING.getValue(), JobPhaseStatus.PAUSING.getValue(), "pause");

        } else {
            throw new AutoexecJobCanNotPauseException();
        }
    }

    /**
     * 中止作业
     *
     * @param jobVo 作业
     */
    @Override
    public void abort(AutoexecJobVo jobVo) {
        new AutoexecJobAuthActionManager.Builder().addAbortJob().build().setAutoexecJobAction(jobVo);
        if (jobVo.getIsCanJobAbort() == 1) {
            abortOrPauseService(jobVo, JobStatus.ABORTING.getValue(), JobPhaseStatus.ABORTING.getValue(), "abort");

        } else {
            throw new AutoexecJobCanNotAbortException();
        }
    }

    /**
     * 执行取消|暂停逻辑
     *
     * @param jobVo       作业vo
     * @param jobStatus   作业状态
     * @param phaseStatus 作业剧本状态
     * @param action      执行动作 取消|暂停
     */
    private void abortOrPauseService(AutoexecJobVo jobVo, String jobStatus, String phaseStatus, String action) {
        jobVo.setStatus(jobStatus);
        autoexecJobMapper.updateJobStatus(jobVo);
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            if (Objects.equals(jobPhase.getStatus(), JobPhaseStatus.RUNNING.getValue()) || Objects.equals(jobPhase.getStatus(), JobPhaseStatus.WAITING.getValue())) {
                jobPhase.setStatus(phaseStatus);
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
                autoexecJobMapper.updateBatchJobPhaseRunnerStatus(jobPhase.getId(), phaseStatus);
            }
        }
        List<AutoexecRunnerVo> runnerVos = checkRunnerHealth(jobVo);
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        //TODO 校验连通性
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        try {
            for (AutoexecRunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/" + action;
                restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
                result = RestUtil.sendRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            assert restVo != null;
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
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
    public List<AutoexecJobPhaseNodeOperationStatusVo> getNodeOperationStatus(JSONObject paramJson) {
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
            JSONObject statusJson = null;
            String resultStr = resultJson.getString("Return");
            if (StringUtils.isNotBlank(resultStr)) {
                statusJson = JSONObject.parseObject(resultStr);
            } else {
                statusJson = new JSONObject();
            }
            List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(paramJson.getLong("jobId"), paramJson.getLong("phaseId"));
            for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                statusList.add(new AutoexecJobPhaseNodeOperationStatusVo(operationVo, statusJson));
            }
        }
        return statusList.stream().sorted(Comparator.comparing(AutoexecJobPhaseNodeOperationStatusVo::getSort)).collect(Collectors.toList());
    }

    @Override
    public JSONArray getNodeOutputParam(JSONObject paramJson) {
        JSONArray operationOutputParamArray = null;
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/output/param/get";
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
            Long jobId = paramJson.getLong("jobId");
            Long jobPhaseId = paramJson.getLong("phaseId");
            List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobId, jobPhaseId);
            List<AutoexecJobParamContentVo> paramContentVoList = autoexecJobMapper.getJobParamContentList(operationVoList.stream().map(AutoexecJobPhaseOperationVo::getParamHash).collect(Collectors.toList()));
            operationOutputParamArray = new JSONArray() {{
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    add(new JSONObject() {{
                        put("name", operationVo.getName());
                        if (StringUtils.isNotBlank(resultStr)) {
                            JSONObject outputParamJson = JSONObject.parseObject(resultStr);
                            List<AutoexecJobParamVo> outputParamList = new ArrayList<>();
                            List<AutoexecJobParamVo> finalOutputParamList = outputParamList;
                            paramContentVoList.forEach(o -> {
                                if (Objects.equals(operationVo.getParamHash(), o.getHash())) {
                                    JSONObject json = JSONObject.parseObject(o.getContent());
                                    JSONArray outputArray = json.getJSONArray("outputParamList");
                                    for (Object output : outputArray) {
                                        AutoexecJobParamVo outputVo = new AutoexecJobParamVo(JSONObject.parseObject(output.toString()));
                                        JSONObject valueJson = outputParamJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
                                        if (valueJson != null) {
                                            outputVo.setValue(valueJson.getString(outputVo.getKey()));
                                        }
                                        finalOutputParamList.add(outputVo);
                                    }
                                }
                            });
                            outputParamList = outputParamList.stream().sorted(Comparator.comparing(AutoexecJobParamVo::getSort)).collect(Collectors.toList());
                            put("paramList", outputParamList);
                        }
                    }});
                }
            }};
        }
        return operationOutputParamArray;
    }
}
