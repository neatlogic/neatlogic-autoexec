/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.process.dto;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
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

    @EntityField(name = "作业执行参数", type = ApiParamType.JSONOBJECT)
    private AutoexecCombopExecuteConfigVo executeConfig;

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

    public AutoexecCombopExecuteConfigVo getExecuteConfig() {
        return executeConfig;
    }

    public void setExecuteConfig(AutoexecCombopExecuteConfigVo executeConfig) {
        this.executeConfig = executeConfig;
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

    public AutoexecJobBuilder(Long combopId) {
        this.combopId = combopId;
    }

    public AutoexecJobVo build() {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setParam(param);
        jobVo.setRunnerGroup(runnerGroup);
        jobVo.setRunnerGroupTag(runnerGroupTag);
        jobVo.setScenarioId(scenarioId);
        jobVo.setExecuteConfig(executeConfig);
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
