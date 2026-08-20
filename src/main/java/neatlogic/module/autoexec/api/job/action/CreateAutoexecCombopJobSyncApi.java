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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecParallelPolicy;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecRunnerGroupTagInvalidException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopVersionNotFoundEditTargetException;
import neatlogic.framework.autoexec.exception.job.AutoexecJobSyncInterruptedException;
import neatlogic.framework.autoexec.exception.job.AutoexecJobTargetInvalidException;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.IpUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Example;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.annotation.ResubmitInterval;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dto.job.AutoexecJobNodeOutputVo;
import neatlogic.module.autoexec.dto.job.AutoexecJobSyncResultVo;
import neatlogic.module.autoexec.job.sync.AutoexecJobSyncManager;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecCombopJobSyncApi extends PrivateApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(CreateAutoexecCombopJobSyncApi.class);
    private static final String NODE_OUTPUT_COLLECTION = "_node_output";
    private static final long POLL_INTERVAL_MS = 1000L;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private AutoexecJobSyncManager autoexecJobSyncManager;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "nmaaja.createautoexeccombopjobsyncapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopName", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexeccombopjobsyncapi.input.param.desc.combopname", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.combopname"),
            @Param(name = "jobName", type = ApiParamType.STRING, desc = "nmaaja.createautoexeccombopjobsyncapi.input.param.desc.jobname", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.jobname"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeparam", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.param"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.scenarioname", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.scenarioname"),
            @Param(name = "targets", type = ApiParamType.JSONARRAY, desc = "nmaaja.createautoexeccombopjobsyncapi.input.param.desc.targets", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.targets"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "nmaaja.createautoexeccombopjobsyncapi.input.param.desc.protocol", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.protocol"),
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "term.autoexec.executeuser", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.executeuser"),
            @Param(name = "runnerGroup", type = ApiParamType.STRING, desc = "common.runnergroup", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.runnergroup"),
            @Param(name = "runnerGroupTag", type = ApiParamType.STRING, desc = "common.runnergrouptag", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.runnergrouptag"),
            @Param(name = "parallelPolicy", type = ApiParamType.ENUM, member = AutoexecParallelPolicy.class, desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelpolicy", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.parallelpolicy"),
            @Param(name = "roundCount", type = ApiParamType.INTEGER, desc = "nmaaja.createautoexeccombopjobapi.input.param.roundcount", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.roundcount"),
            @Param(name = "parallelCount", type = ApiParamType.INTEGER, desc = "nmaaja.createautoexeccombopjobapi.input.param.desc.parallelcount", help = "nmaaja.createautoexeccombopjobsyncapi.input.param.help.parallelcount")
    })
    @Output({
            @Param(explode = AutoexecJobSyncResultVo.class)
    })
    @Description(desc = "nmaaja.createautoexeccombopjobsyncapi.description.desc")
    @Example(example = "{\"combopName\":\"deploy\",\"targets\":[\"192.168.1.10:22/app\"],\"protocol\":\"ssh\",\"executeUser\":\"root\"}")
    @ResubmitInterval(value = 5)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String tenantUuid = TenantContext.get().getTenantUuid();
        autoexecJobSyncManager.acquire(tenantUuid);
        Long jobId = null;
        try {
            AutoexecJobVo jobVo = buildJob(paramObj);
            // 作业保存方法自行控制短事务；接口层不持有事务，提交后才激活并轮询。
            autoexecJobActionService.validateAndCreateJobFromCombop(jobVo);
            jobId = jobVo.getId();
            autoexecJobSyncManager.registerJob(tenantUuid, jobId, jobVo.getName());
            autoexecJobActionService.settingJobFireMode(jobVo);
            return waitForTerminal(jobVo, System.currentTimeMillis());
        } finally {
            autoexecJobSyncManager.release(tenantUuid, jobId);
        }
    }

    /**
     * 使用组合工具活动版本构建作业，并仅覆盖调用方显式提供的执行配置。
     */
    private AutoexecJobVo buildJob(JSONObject paramObj) {
        String combopName = paramObj.getString("combopName");
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopByName(combopName);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopName);
        }
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopVo.getId());
        if (activeVersionId == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(combopName);
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
        if (versionVo == null) {
            throw new AutoexecCombopVersionNotFoundEditTargetException(activeVersionId);
        }
        AutoexecCombopVersionConfigVo versionConfig = versionVo.getConfig();
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setName(StringUtils.defaultIfBlank(paramObj.getString("jobName"), combopName));
        jobVo.setParam(initParam(paramObj.getJSONObject("param"), versionConfig));
        jobVo.setScenarioName(paramObj.getString("scenarioName"));
        jobVo.setParallelPolicy(paramObj.getString("parallelPolicy"));
        jobVo.setRoundCount(paramObj.getInteger("roundCount"));
        jobVo.setParallelCount(paramObj.getInteger("parallelCount"));
        jobVo.setExecUser(UserContext.get().getUserUuid());
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setSource(JobSource.COMBOP.getValue());
        jobVo.setOperationId(combopVo.getId());
        jobVo.setCombopVersionId(activeVersionId);
        jobVo.setRunnerGroup(buildConstantMapping(paramObj.getString("runnerGroup")));
        jobVo.setRunnerGroupTag(buildRunnerGroupTagMapping(paramObj.getString("runnerGroupTag")));
        jobVo.setExecuteConfig(buildExecuteConfig(paramObj));
        if (versionConfig != null && versionConfig.getExecuteConfig() != null) {
            jobVo.setPreCondition(versionConfig.getExecuteConfig().getPreCondition());
            jobVo.setWhenToSpecify(versionConfig.getExecuteConfig().getWhenToSpecify());
        }
        return jobVo;
    }

    /**
     * 按组合工具运行参数定义补齐默认值并执行既有类型转换。
     */
    private JSONObject initParam(JSONObject inputParam, AutoexecCombopVersionConfigVo versionConfig) {
        JSONObject param = inputParam == null ? new JSONObject() : inputParam;
        JSONObject result = new JSONObject();
        if (versionConfig == null || CollectionUtils.isEmpty(versionConfig.getRuntimeParamList())) {
            return result;
        }
        List<AutoexecParamVo> runtimeParamList = versionConfig.getRuntimeParamList().stream()
                .filter(o -> !Objects.equals(ParamType.FILE.getValue(), o.getType()))
                .collect(Collectors.toList());
        for (AutoexecParamVo paramVo : runtimeParamList) {
            Object value = param.containsKey(paramVo.getKey()) && param.get(paramVo.getKey()) != null
                    ? param.get(paramVo.getKey()) : paramVo.getDefaultValue();
            IScriptParamType paramType = ScriptParamTypeFactory.getHandler(paramVo.getType());
            if (paramType != null) {
                value = paramType.getExchangeParamByValue(value);
            }
            result.put(paramVo.getKey(), value);
        }
        return result;
    }

    /**
     * 将模型友好的扁平字段转换为现有组合工具执行配置。
     */
    private AutoexecCombopExecuteConfigVo buildExecuteConfig(JSONObject paramObj) {
        boolean hasProtocol = StringUtils.isNotBlank(paramObj.getString("protocol"));
        boolean hasExecuteUser = StringUtils.isNotBlank(paramObj.getString("executeUser"));
        boolean hasTargets = paramObj.containsKey("targets");
        if (!hasProtocol && !hasExecuteUser && !hasTargets) {
            return null;
        }
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        if (hasProtocol) {
            String protocol = paramObj.getString("protocol");
            IResourceAccountCrossoverMapper mapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountProtocolVo protocolVo = mapper.getAccountProtocolVoByProtocolName(protocol);
            if (protocolVo == null) {
                throw new ResourceCenterAccountProtocolNotFoundException(protocol);
            }
            executeConfig.setProtocol(protocol);
            executeConfig.setProtocolId(protocolVo.getId());
        }
        if (hasExecuteUser) {
            executeConfig.setExecuteUser(buildConstantMapping(paramObj.getString("executeUser")));
        }
        if (hasTargets) {
            AutoexecCombopExecuteNodeConfigVo nodeConfig = new AutoexecCombopExecuteNodeConfigVo();
            nodeConfig.setInputNodeList(parseTargets(paramObj.getJSONArray("targets")));
            executeConfig.setExecuteNodeConfig(nodeConfig);
        }
        return executeConfig;
    }

    private ParamMappingVo buildConstantMapping(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        ParamMappingVo mappingVo = new ParamMappingVo();
        mappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
        mappingVo.setValue(value);
        return mappingVo;
    }

    /**
     * 单标签和 JSON 数组字符串最终都保存成合法的 JSON 数组字符串。
     */
    private ParamMappingVo buildRunnerGroupTagMapping(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        List<String> tagList;
        if (value.trim().startsWith("[")) {
            try {
                tagList = JSON.parseArray(value, String.class);
            } catch (Exception ex) {
                logger.error("Invalid runner group tag JSON array: {}", value, ex);
                throw new AutoexecRunnerGroupTagInvalidException(value);
            }
        } else {
            tagList = Collections.singletonList(value);
        }
        if (CollectionUtils.isEmpty(tagList) || tagList.stream().anyMatch(StringUtils::isBlank)) {
            throw new AutoexecRunnerGroupTagInvalidException(value);
        }
        ParamMappingVo mappingVo = new ParamMappingVo();
        mappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
        mappingVo.setValue(JSON.toJSONString(tagList));
        return mappingVo;
    }

    /**
     * 解析 IP、IP:port、IP:port/name 三种目标格式。
     */
    private List<AutoexecNodeVo> parseTargets(JSONArray targets) {
        if (CollectionUtils.isEmpty(targets)) {
            throw new AutoexecJobTargetInvalidException(String.valueOf(targets));
        }
        List<AutoexecNodeVo> nodeList = new ArrayList<>();
        for (Object targetObject : targets) {
            if (!(targetObject instanceof String) || StringUtils.isBlank((String) targetObject)) {
                throw new AutoexecJobTargetInvalidException(String.valueOf(targetObject));
            }
            String target = ((String) targetObject).trim();
            if (!target.matches("[^:/\\s]+(?::(?:[1-9]\\d{0,4})(?:/[^/\\s]+)?)?")) {
                throw new AutoexecJobTargetInvalidException(target);
            }
            try {
                AutoexecNodeVo nodeVo = new AutoexecNodeVo(target);
                if (nodeVo.getIp().contains("*") || !IpUtil.checkIp(nodeVo.getIp())
                        || (nodeVo.getPort() != null && nodeVo.getPort() > 65535)) {
                    throw new AutoexecJobTargetInvalidException(target);
                }
                nodeList.add(nodeVo);
            } catch (NumberFormatException ex) {
                logger.error("Invalid autoexec job target: {}", target, ex);
                throw new AutoexecJobTargetInvalidException(target);
            }
        }
        return nodeList;
    }

    /**
     * 每秒读取一次作业状态，终止或到达租户超时时间后返回。
     */
    private AutoexecJobSyncResultVo waitForTerminal(AutoexecJobVo createdJob, long startTime) {
        long deadline = startTime + autoexecJobSyncManager.getWaitTimeout();
        AutoexecJobVo currentJob;
        boolean timedOut = false;
        while (true) {
            currentJob = autoexecJobMapper.getJobInfo(createdJob.getId());
            if (currentJob == null) {
                throw new AutoexecJobNotFoundException(String.valueOf(createdJob.getId()));
            }
            if (isTerminal(currentJob.getStatus())) {
                break;
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                timedOut = true;
                break;
            }
            try {
                Thread.sleep(Math.min(POLL_INTERVAL_MS, remaining));
            } catch (InterruptedException ex) {
                logger.error("Interrupted while waiting for autoexec job status, jobId: {}", createdJob.getId(), ex);
                Thread.currentThread().interrupt();
                throw new AutoexecJobSyncInterruptedException(createdJob.getId());
            }
        }
        AutoexecJobSyncResultVo result = buildResult(currentJob, timedOut, System.currentTimeMillis() - startTime);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            readNodeOutput(result);
        }
        return result;
    }

    private boolean isTerminal(String status) {
        return JobStatus.isCompletedStatus(status) || JobStatus.isFailedStatus(status);
    }

    private AutoexecJobSyncResultVo buildResult(AutoexecJobVo jobVo, boolean timedOut, long waitTimeMs) {
        AutoexecJobSyncResultVo result = new AutoexecJobSyncResultVo();
        result.setJobId(jobVo.getId());
        result.setJobName(jobVo.getName());
        result.setStatus(jobVo.getStatus());
        result.setStatusName(JobStatus.getText(jobVo.getStatus()));
        result.setTerminal(isTerminal(jobVo.getStatus()));
        result.setSuccess(JobStatus.isCompletedStatus(jobVo.getStatus()));
        result.setTimedOut(timedOut);
        result.setWaitTimeMs(waitTimeMs);
        result.setOutputTargetCount(0);
        result.setReturnedOutputTargetCount(0);
        result.setOutputTruncated(false);
        result.setOutputReadSuccess(false);
        result.setOutputReadMessage(StringUtils.EMPTY);
        result.setNodeOutputList(Collections.emptyList());
        return result;
    }

    /**
     * 成功后读取有限数量的节点输出；读取失败不改变已成功的作业结果。
     */
    private void readNodeOutput(AutoexecJobSyncResultVo result) {
        try {
            Query countQuery = Query.query(Criteria.where("jobId").is(String.valueOf(result.getJobId())));
            long outputTargetCount = mongoTemplate.count(countQuery, NODE_OUTPUT_COLLECTION);
            int maxTargetCount = autoexecJobSyncManager.getOutputMaxTargetCount();
            Query outputQuery = Query.query(Criteria.where("jobId").is(String.valueOf(result.getJobId())))
                    .with(Sort.by(Sort.Order.asc("resourceId"), Sort.Order.asc("_id")))
                    .limit(maxTargetCount);
            List<JSONObject> outputList = mongoTemplate.find(outputQuery, JSONObject.class, NODE_OUTPUT_COLLECTION);
            List<AutoexecJobNodeOutputVo> nodeOutputList = new ArrayList<>();
            for (JSONObject output : outputList) {
                AutoexecJobNodeOutputVo nodeOutputVo = new AutoexecJobNodeOutputVo();
                Long resourceId = output.getLong("resourceId");
                nodeOutputVo.setResourceId(resourceId);
                nodeOutputVo.setHost(Objects.equals(resourceId, 0L) ? "local" : output.getString("host"));
                nodeOutputVo.setPort(output.getInteger("port"));
                nodeOutputVo.setOutput(output.get("data"));
                nodeOutputList.add(nodeOutputVo);
            }
            result.setOutputTargetCount((int) Math.min(Integer.MAX_VALUE, outputTargetCount));
            result.setReturnedOutputTargetCount(nodeOutputList.size());
            result.setOutputTruncated(outputTargetCount > nodeOutputList.size());
            result.setOutputReadSuccess(true);
            result.setNodeOutputList(nodeOutputList);
        } catch (Exception ex) {
            logger.error("Failed to read autoexec job node output from MongoDB, jobId: {}", result.getJobId(), ex);
            result.setOutputReadSuccess(false);
            result.setOutputReadMessage($.t("nmaaja.createautoexeccombopjobsyncapi.message.outputreadfailed"));
        }
    }

    @Override
    public String getToken() {
        return "/autoexec/combop/job/create/sync";
    }
}
