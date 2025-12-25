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

package neatlogic.module.autoexec.process.dto;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.EntityField;
import org.apache.commons.lang3.StringUtils;

public class AutoexecJobBuilder {
    @EntityField(name = "组合工具id", type = ApiParamType.LONG)
    private final Long combopId;
    @EntityField(name = "作业名称（唯一标识）", type = ApiParamType.STRING)
    private String jobName;

    @EntityField(name = "场景id", type = ApiParamType.LONG)
    private Long scenarioId;

//    @EntityField(name = "作业执行参数", type = ApiParamType.JSONOBJECT)
//    private AutoexecCombopExecuteConfigVo executeConfig;

    @EntityField(name = "协议id", type = ApiParamType.LONG)
    private Long protocolId;

    @EntityField(name = "执行用户", type = ApiParamType.JSONOBJECT)
    private ParamMappingVo executeUser;

    @EntityField(name = "执行目标配置", type = ApiParamType.JSONOBJECT)
    private AutoexecCombopExecuteNodeConfigVo executeNodeConfig;

    @EntityField(name = "如何指定执行目标，（现在指定执行目标、运行时再指定执行目标、运行参数作为执行目标）", type = ApiParamType.STRING)
    private String whenToSpecify;

    @EntityField(name = "前置执行目标配置", type = ApiParamType.JSONOBJECT)
    private JSONObject preCondition;

    @EntityField(name = "runner执行组", type = ApiParamType.JSONOBJECT)
    private ParamMappingVo runnerGroup;

    @EntityField(name = "runner执行组标签", type = ApiParamType.JSONOBJECT)
    private ParamMappingVo runnerGroupTag;

    @EntityField(name = "分批数", type = ApiParamType.INTEGER)
    private Integer roundCount;

    @EntityField(name = "并发线程数", type = ApiParamType.INTEGER)
    private Integer parallelCount;

    @EntityField(name = "并发策略", type = ApiParamType.INTEGER)
    private String parallelPolicy;

    @EntityField(name = "作业参数数据", type = ApiParamType.JSONOBJECT)
    private JSONObject param;

    private String error;

    public Long getCombopId() {
        return combopId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

//    public AutoexecCombopExecuteConfigVo getExecuteConfig() {
//        return executeConfig;
//    }
//
//    public void setExecuteConfig(AutoexecCombopExecuteConfigVo executeConfig) {
//        this.executeConfig = executeConfig;
//    }

    public Long getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(Long protocolId) {
        this.protocolId = protocolId;
    }

    public ParamMappingVo getExecuteUser() {
        return executeUser;
    }

    public void setExecuteUser(ParamMappingVo executeUser) {
        this.executeUser = executeUser;
    }

    public AutoexecCombopExecuteNodeConfigVo getExecuteNodeConfig() {
        return executeNodeConfig;
    }

    public void setExecuteNodeConfig(AutoexecCombopExecuteNodeConfigVo executeNodeConfig) {
        this.executeNodeConfig = executeNodeConfig;
    }

    public String getWhenToSpecify() {
        return whenToSpecify;
    }

    public void setWhenToSpecify(String whenToSpecify) {
        this.whenToSpecify = whenToSpecify;
    }

    public JSONObject getPreCondition() {
        return preCondition;
    }

    public void setPreCondition(JSONObject preCondition) {
        this.preCondition = preCondition;
    }

    public ParamMappingVo getRunnerGroup() {
        return runnerGroup;
    }

    public void setRunnerGroup(ParamMappingVo runnerGroup) {
        this.runnerGroup = runnerGroup;
    }

    public ParamMappingVo getRunnerGroupTag() {
        return runnerGroupTag;
    }

    public void setRunnerGroupTag(ParamMappingVo runnerGroupTag) {
        this.runnerGroupTag = runnerGroupTag;
    }

    public Integer getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(Integer roundCount) {
        this.roundCount = roundCount;
    }

    public JSONObject getParam() {
        return param;
    }

    public void setParam(JSONObject param) {
        this.param = param;
    }

    public Integer getParallelCount() {
        return parallelCount;
    }

    public void setParallelCount(Integer parallelCount) {
        this.parallelCount = parallelCount;
    }

    public String getParallelPolicy() {
        return parallelPolicy;
    }

    public void setParallelPolicy(String parallelPolicy) {
        this.parallelPolicy = parallelPolicy;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public AutoexecJobBuilder(Long combopId) {
        this.combopId = combopId;
    }

    public AutoexecJobVo build() {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setParam(param);
        jobVo.setRunnerGroup(runnerGroup);
        jobVo.setRunnerGroupTag(runnerGroupTag);
        jobVo.setScenarioId(scenarioId);
//        jobVo.setExecuteConfig(executeConfig);
        jobVo.setProtocolId(protocolId);
        jobVo.setExecuteUser(executeUser);
        jobVo.setExecuteNodeConfig(executeNodeConfig);
        jobVo.setWhenToSpecify(whenToSpecify);
        jobVo.setPreCondition(preCondition);
        if (roundCount != null) {
            jobVo.setRoundCount(roundCount);
        }
        if (parallelCount != null) {
            jobVo.setParallelCount(parallelCount);
        }
        if(StringUtils.isNotBlank(parallelPolicy)){
            jobVo.setParallelPolicy(parallelPolicy);
        }
        jobVo.setName(jobName);
        jobVo.setOperationId(combopId);
        return jobVo;
    }
}
