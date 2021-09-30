/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RestVo;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.integration.authentication.costvalue.AuthenticateType;
import codedriver.framework.util.RestUtil;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
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

    @Resource
    ResourceCenterMapper resourceCenterMapper;


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
    private List<RunnerVo> checkRunnerHealth(AutoexecJobVo jobVo) {
        List<RunnerVo> runnerVos;
        RestVo restVo;
        String result;
        String url;
        runnerVos = autoexecJobMapper.getJobPhaseRunnerByJobIdAndPhaseIdList(jobVo.getId(), jobVo.getPhaseIdList());
        for (RunnerVo runner : runnerVos) {
            url = runner.getUrl() + "api/rest/health/check";
            restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), new JSONObject());
            result = RestUtil.sendRequest(restVo);
            if (JSONValidator.from(result).validate()) {
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            } else {
                throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + result);
            }
        }
        return runnerVos;
    }

    /**
     * 执行作业阶段
     *
     * @param jobVo 作业
     */
    private void execute(AutoexecJobVo jobVo) {
        jobVo.setStatus(JobStatus.RUNNING.getValue());
        autoexecJobMapper.updateJobStatus(jobVo);
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            jobPhase.setStatus(JobPhaseStatus.WAITING.getValue());
            autoexecJobMapper.updateJobPhaseStatus(jobPhase);
        }

        JSONObject paramJson = new JSONObject();
        getFireParamJson(paramJson, jobVo);
        paramJson.put("isFirstFire", jobVo.getCurrentPhaseSort() == 0 ? 1 : 0);

        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        try {
            List<RunnerVo> runnerVos = checkRunnerHealth(jobVo);
            for (RunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/exec";
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getId());
                    put("phaseSort", jobVo.getCurrentPhaseSort());
                }});
                restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), paramJson);
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

    /**
     * 执行
     *
     * @param jobVo 作业
     */
    @Override
    public void fire(AutoexecJobVo jobVo) {
        new AutoexecJobAuthActionManager.Builder().addFireJob().build().setAutoexecJobAction(jobVo);
        autoexecJobMapper.getJobLockByJobId(jobVo.getId());
        execute(jobVo);
    }

    @Override
    public void refire(AutoexecJobVo jobVo, String type) {
        jobVo.setAction(type);
        if (Objects.equals(type, JobAction.RESET_REFIRE.getValue())) {
            if(CollectionUtils.isEmpty(jobVo.getPhaseList())){
                jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()));
            }
            new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
            resetAll(jobVo);
            autoexecJobMapper.updateJobPhaseStatusByJobId(jobVo.getId(), JobPhaseStatus.PENDING.getValue());//重置phase状态为pending
            autoexecJobMapper.updateJobPhaseFailedNodeStatusByJobId(jobVo.getId(), JobNodeStatus.PENDING.getValue());
            autoexecJobService.getAutoexecJobDetail(jobVo, 0);
            jobVo.setCurrentPhaseSort(0);
            autoexecJobService.refreshJobPhaseNodeList(jobVo.getId(), jobVo.getCurrentPhaseSort(), null);
        } else if (Objects.equals(type, JobAction.REFIRE.getValue())) {
            int sort = 0;
            /*寻找中止|暂停|失败的phase
             * 1、优先寻找pending|aborted|paused|failed phaseList
             * 2、没有满足1条件的，再寻找pending|aborted|paused|failed node 最小sort phaseList
             */
            List<AutoexecJobPhaseVo> autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobVo.getId(), Arrays.asList(JobPhaseStatus.PENDING.getValue(),JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            if (CollectionUtils.isEmpty(autoexecJobPhaseVos)) {
                autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndNodeStatusList(jobVo.getId(),Arrays.asList(JobPhaseStatus.PENDING.getValue(),JobPhaseStatus.ABORTED.getValue(), JobPhaseStatus.PAUSED.getValue(), JobPhaseStatus.FAILED.getValue()));
            }
            sort = autoexecJobPhaseVos.get(0).getSort();
            //int finalSort = sort;
            //List<Long> jobPhaseIdList = autoexecJobPhaseVos.stream().filter(p->p.getSort() == finalSort).map(AutoexecJobPhaseVo::getId).collect(Collectors.toList());
            jobVo.setCurrentPhaseSort(sort);
            autoexecJobService.getAutoexecJobDetail(jobVo, sort);
            //补充配置，只保留满足条件（该sort下，未开始、失败、已暂停或已中止）的phase
            //jobVo.setPhaseList(jobVo.getPhaseList().stream().filter(o -> jobPhaseIdList.contains(o.getId())).collect(Collectors.toList()));
            if(CollectionUtils.isNotEmpty(jobVo.getPhaseList())){
                new AutoexecJobAuthActionManager.Builder().addReFireJob().build().setAutoexecJobAction(jobVo);
            }
        } else if (Objects.equals(type, JobAction.REFIRE_NODE.getValue())) {
            List<AutoexecJobPhaseNodeVo> nodeVoList = jobVo.getNodeVoList();
            AutoexecJobPhaseNodeVo nodeVo = nodeVoList.get(0);
            for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : nodeVoList) {
                if (!Objects.equals(jobPhaseNodeVo.getJobPhaseId(), nodeVo.getJobPhaseId())) {
                    throw new ParamIrregularException("nodeIdList");
                }
            }
            AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(nodeVo.getJobId(), nodeVo.getJobPhaseId());
            jobVo.setCurrentPhaseSort(phaseVo.getSort());
            autoexecJobService.getAutoexecJobDetail(jobVo, phaseVo.getSort());
            //过滤仅需要当前phase的配置
            jobVo.setPhaseList(jobVo.getPhaseList().stream().filter(o -> Objects.equals(phaseVo.getId(), o.getId())).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(jobVo.getPhaseList())) {
            execute(jobVo);
        }

    }

    /**
     * 重置作业
     *
     * @param jobVo 作业
     */
    private void resetAll(AutoexecJobVo jobVo) {
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        List<RunnerVo> runnerVos = checkRunnerHealth(jobVo);
        try {
            for (RunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/all/reset";
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getId());
                    put("phaseSort", jobVo.getCurrentPhaseSort());
                }});
                restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), paramJson);
                result = RestUtil.sendRequest(restVo);
                JSONObject resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            }
        } catch (Exception ex) {
            throw new AutoexecJobRunnerConnectRefusedException(restVo.getUrl() + " " + result);
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
        //工具库测试|重跑节点
        if (CollectionUtils.isNotEmpty(jobVo.getNodeVoList())) {
            paramJson.put("noFireNext", 1);
            List<AutoexecJobPhaseNodeVo> nodeVoList = jobVo.getNodeVoList();
            String protocol = nodeVoList.get(0).getProtocol();
            String userName = nodeVoList.get(0).getUserName();
            paramJson.put("runNode", new JSONArray() {{
                Map<Long, AccountVo> resourceAccountMap = new HashMap<>();
                List<AccountVo> accountVoList = resourceCenterMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).collect(Collectors.toList()), protocol, userName);
                accountVoList.forEach(o -> {
                    resourceAccountMap.put(o.getResourceId(), o);
                });
                for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
                    add(new JSONObject() {{
                        AccountVo accountVo = resourceAccountMap.get(nodeVo.getResourceId());
                        put("protocol", accountVo.getProtocol());
                        put("username", accountVo.getAccount());
                        put("password", accountVo.getPasswordPlain());
                        put("protocolPort", accountVo.getPort());
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
        return JSONObject.parseObject(requestRunner(url, paramJson));
    }

    @Override
    public void downloadNodeLog(JSONObject paramJson, HttpServletResponse response) throws IOException {
        String runnerUrl = paramJson.getString("runnerUrl");
        Long jobId = paramJson.getLong("jobId");
        String phase = paramJson.getString("phase");
        String ip = paramJson.getString("ip");
        String port = StringUtils.EMPTY;
        String execMode = paramJson.getString("execMode");
        String resourceId = paramJson.getString("resourceId");
        if(paramJson.getInteger("port") != null){
            port = "port="+paramJson.getInteger("port");
        }
        String url = String.format("%s/api/binary/job/phase/node/log/download?jobId=%s&phase=%s&ip=%s&%s&execMode=%s&resourceId=%s", runnerUrl, jobId, HttpUtils.urlEncode(phase), ip, port, execMode,resourceId);
        RestVo restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), paramJson);
        String result = RestUtil.sendGetRequestForStream(restVo, response);
        if(StringUtils.isNotBlank(result)) {
            throw new AutoexecJobRunnerConnectAuthException(restVo.getUrl() + ":" + result);
        }
    }

    @Override
    public JSONObject tailConsoleLog(JSONObject paramJson) {
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/console/log/tail";
        return JSONObject.parseObject(requestRunner(url, paramJson));
    }

    /**
     * 暂停作业
     *
     * @param jobVo 作业
     */
    @Override
    public void pause(AutoexecJobVo jobVo) {
        new AutoexecJobAuthActionManager.Builder().addPauseJob().build().setAutoexecJobAction(jobVo);
        abortOrPauseService(jobVo, JobStatus.PAUSING.getValue(), JobPhaseStatus.PAUSING.getValue(), "pause");
    }

    /**
     * 中止作业
     *
     * @param jobVo 作业
     */
    @Override
    public void abort(AutoexecJobVo jobVo) {
        new AutoexecJobAuthActionManager.Builder().addAbortJob().build().setAutoexecJobAction(jobVo);
        abortOrPauseService(jobVo, JobStatus.ABORTING.getValue(), JobPhaseStatus.ABORTING.getValue(), "abort");
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
        //更新job状态 为中止中
        autoexecJobMapper.updateJobStatus(jobVo);
        //更新phase状态 为中止中
        for (AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
            if (Objects.equals(jobPhase.getStatus(), JobPhaseStatus.RUNNING.getValue()) || Objects.equals(jobPhase.getStatus(), JobPhaseStatus.WAITING.getValue())) {
                jobPhase.setStatus(phaseStatus);
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
                autoexecJobMapper.updateBatchJobPhaseRunnerStatus(jobPhase.getId(), phaseStatus);
            }
        }
        //更新node状态 为中止中
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndNodeStatusList(jobVo.getId(), Collections.singletonList(JobNodeStatus.RUNNING.getValue()));
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            nodeVo.setStatus(JobNodeStatus.ABORTING.getValue());
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }
        List<RunnerVo> runnerVos = checkRunnerHealth(jobVo);
        JSONObject paramJson = new JSONObject();
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("tenant", TenantContext.get().getTenantUuid());
        paramJson.put("execUser", UserContext.get().getUserUuid(true));
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        try {
            for (RunnerVo runner : runnerVos) {
                paramJson.put("passThroughEnv", new JSONObject() {{
                    put("runnerId", runner.getId());
                    put("phaseSort", jobVo.getCurrentPhaseSort());
                }});
                String url = runner.getUrl() + "api/rest/job/" + action;
                restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), paramJson);
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
     * @param jobVo 作业
     */
    @Override
    public void resetNode(AutoexecJobVo jobVo) {
        //更新作业状态
        //autoexecJobMapper.updateJobStatus(new AutoexecJobVo(jobVo.getId(),JobStatus.RUNNING.getValue()));
        //更新阶段状态
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getPhaseList().get(0);
        /*List<String> exceptStatus = Collections.singletonList(JobNodeStatus.IGNORED.getValue());
        List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatus(currentPhaseVo.getJobId(), currentPhaseVo.getId(), exceptStatus);
        if(CollectionUtils.isNotEmpty(jobPhaseNodeVoList)&&jobPhaseNodeVoList.size() == 1){//如果该阶段只有一个节点
            currentPhaseVo.setStatus(JobPhaseStatus.PENDING.getValue());
        }else{
            currentPhaseVo.setStatus(JobPhaseStatus.RUNNING.getValue());
        }
        autoexecJobMapper.updateJobPhaseStatus(currentPhaseVo);*/
        //重置节点 (status、starttime、endtime)
        for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getJobPhaseNodeList()) {
            nodeVo.setStatus(JobNodeStatus.PENDING.getValue());
            nodeVo.setStartTime(null);
            nodeVo.setEndTime(null);
            autoexecJobMapper.updateJobPhaseNode(nodeVo);
        }
        autoexecJobMapper.updateJobPhaseNodeListStatus(jobVo.getJobPhaseNodeList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()), JobNodeStatus.PENDING.getValue());

        //清除runner node状态
        List<RunnerVo> runnerVos = checkRunnerHealth(jobVo);
        RestVo restVo = null;
        String result = StringUtils.EMPTY;
        try {
            JSONObject paramJson = new JSONObject();
            paramJson.put("jobId", jobVo.getId());
            paramJson.put("tenant", TenantContext.get().getTenantUuid());
            paramJson.put("execUser", UserContext.get().getUserUuid(true));
            paramJson.put("phaseName", currentPhaseVo.getName());
            paramJson.put("execMode", currentPhaseVo.getExecMode());
            paramJson.put("phaseNodeList", jobVo.getJobPhaseNodeList());
            for (RunnerVo runner : runnerVos) {
                String url = runner.getUrl() + "api/rest/job/phase/node/status/reset";
                restVo = new RestVo(url, AuthenticateType.BUILDIN.getValue(), paramJson);
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
        List<AutoexecJobPhaseNodeAuditVo> auditList = new ArrayList<>();
        JSONArray auditArray = JSONArray.parseArray(requestRunner(url, paramJson));
        for (Object audit : auditArray) {
            JSONObject auditJson = (JSONObject) audit;
            AutoexecJobPhaseNodeAuditVo auditVo = new AutoexecJobPhaseNodeAuditVo(auditJson);
            auditVo.setExecUserVo(userMapper.getUserBaseInfoByUuidWithoutCache(auditVo.getExecUser()));
            //TODO download
            //auditVo.setDownloadPath(String.format(""));
            auditList.add(auditVo);
        }
        return auditList;
    }

    @Override
    public AutoexecJobPhaseNodeVo getNodeOperationStatus(JSONObject paramJson) {
        List<AutoexecJobPhaseNodeOperationStatusVo> statusList = new ArrayList<>();
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/status/get";
        JSONObject statusJson = JSONObject.parseObject(requestRunner(url, paramJson));
        AutoexecJobPhaseNodeVo nodeVo = new AutoexecJobPhaseNodeVo(statusJson);
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(paramJson.getLong("jobId"), paramJson.getLong("phaseId"));
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            statusList.add(new AutoexecJobPhaseNodeOperationStatusVo(operationVo, statusJson));
        }
        nodeVo.setOperationStatusVoList(statusList.stream().sorted(Comparator.comparing(AutoexecJobPhaseNodeOperationStatusVo::getSort)).collect(Collectors.toList()));
        return nodeVo;
    }

    /**
     * 请求runner
     *
     * @param runnerUrl runner 链接
     * @param paramJson 入参
     * @return runner response
     */
    private String requestRunner(String runnerUrl, JSONObject paramJson) {
        RestVo restVo = new RestVo(runnerUrl, AuthenticateType.BUILDIN.getValue(), paramJson);
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
        }
        return resultJson.getString("Return");
    }

    @Override
    public JSONArray getNodeOutputParam(JSONObject paramJson) {
        JSONArray operationOutputParamArray = null;
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/output/param/get";
        JSONObject statusJson = JSONObject.parseObject(requestRunner(url, paramJson));
        if (MapUtils.isNotEmpty(statusJson)) {
            Long jobId = paramJson.getLong("jobId");
            Long jobPhaseId = paramJson.getLong("phaseId");
            List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobId, jobPhaseId);
            List<AutoexecJobParamContentVo> paramContentVoList = autoexecJobMapper.getJobParamContentList(operationVoList.stream().map(AutoexecJobPhaseOperationVo::getParamHash).collect(Collectors.toList()));
            operationOutputParamArray = new JSONArray() {{
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    add(new JSONObject() {{
                        put("name", operationVo.getName());
                        List<AutoexecJobParamVo> outputParamList = new ArrayList<>();
                        List<AutoexecJobParamVo> finalOutputParamList = outputParamList;
                        paramContentVoList.forEach(o -> {
                            if (Objects.equals(operationVo.getParamHash(), o.getHash())) {
                                JSONObject json = JSONObject.parseObject(o.getContent());
                                JSONArray outputArray = json.getJSONArray("outputParamList");
                                for (Object output : outputArray) {
                                    AutoexecJobParamVo outputVo = new AutoexecJobParamVo(JSONObject.parseObject(output.toString()));
                                    JSONObject valueJson = statusJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
                                    if (valueJson != null) {
                                        outputVo.setValue(valueJson.getString(outputVo.getKey()));
                                    }
                                    finalOutputParamList.add(outputVo);
                                }
                            }
                        });
                        outputParamList = outputParamList.stream().sorted(Comparator.comparing(AutoexecJobParamVo::getSort)).collect(Collectors.toList());
                        put("paramList", outputParamList);
                    }});
                }
            }};
        }
        return operationOutputParamArray;
    }

    /**
     * 获取执行sql状态
     *
     * @param paramJson 参数
     * @return sql执行状态
     */
    @Override
    public AutoexecJobNodeSqlVo getNodeSqlStatus(JSONObject paramJson) {
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/status/get";
        JSONObject statusJson = JSONObject.parseObject(requestRunner(url, paramJson));
        if (MapUtils.isNotEmpty(statusJson)) {
            return new AutoexecJobNodeSqlVo(statusJson);
        }
        return null;
    }

    /**
     * 获取node sql列表
     *
     * @param paramObj 参数
     * @return sql列表
     */
    @Override
    public List<AutoexecJobNodeSqlVo> getNodeSqlList(JSONObject paramObj) {
        String url = paramObj.getString("runnerUrl") + "/api/rest/job/phase/node/sql/list";
        JSONArray sqlArray = JSONObject.parseArray(requestRunner(url, paramObj));
        return sqlArray.toJavaList(AutoexecJobNodeSqlVo.class);
    }

    /**
     * 获取sql文件 内容
     *
     * @param paramObj 参数
     * @return sql文件 内容
     */
    @Override
    public String getNodeSqlContent(JSONObject paramObj) {
        String url = paramObj.getString("runnerUrl") + "/api/rest/job/phase/node/sql/content/get";
        return requestRunner(url, paramObj);
    }

    @Override
    public void submitWaitInput(JSONObject paramObj) {
        String url = paramObj.getString("runnerUrl") + "/api/rest/job/phase/node/submit/waitInput";
        requestRunner(url, paramObj);
    }

    @Override
    public AutoexecJobVo validateCreateJobFromCombop(JSONObject jsonObj, boolean isNeedAuth) {
        Long combopId = jsonObj.getLong("combopId");
        Integer threadCount = jsonObj.getInteger("threadCount");
        JSONObject paramJson = jsonObj.getJSONObject("param");
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
            AutoexecCombopExecuteConfigVo executeConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            combopVo.getConfig().setExecuteConfig(executeConfigVo);
        }
        autoexecCombopService.verifyAutoexecCombopConfig(combopVo);
        //TODO 校验执行参数

        //并发数必须是2的n次方
        if ((threadCount & (threadCount - 1)) != 0) {
            throw new AutoexecJobThreadCountException();
        }
        AutoexecJobInvokeVo invokeVo = new AutoexecJobInvokeVo(jsonObj.getLong("invokeId"), jsonObj.getString("source"));
        AutoexecJobVo jobVo = autoexecJobService.saveAutoexecCombopJob(combopVo, invokeVo, threadCount, paramJson);
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setCurrentPhaseSort(0);
        return jobVo;
    }
}
