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

import com.alibaba.fastjson.JSONArray;

import java.util.List;

public class CreateJobConfigConfigVo {

//    private Long id;
    private Long combopId;

    private String combopName;

    private String createPolicy;

    private String jobNamePrefixMappingValue;

    private String jobName;

    private String formTag;

    private List<CreateJobConfigMappingGroupVo> jobParamMappingGroupList;

    private List<CreateJobConfigMappingGroupVo> executeParamMappingGroupList;

    private CreateJobConfigMappingVo batchDataSourceMapping;

    private JSONArray formAttributeMappingList;

    private List<CreateJobConfigMappingGroupVo> scenarioParamMappingGroupList;

    public Long getCombopId() {
        return combopId;
    }

    public void setCombopId(Long combopId) {
        this.combopId = combopId;
    }

    public String getCombopName() {
        return combopName;
    }

    public void setCombopName(String combopName) {
        this.combopName = combopName;
    }

    public String getCreatePolicy() {
        return createPolicy;
    }

    public void setCreatePolicy(String createPolicy) {
        this.createPolicy = createPolicy;
    }

    public String getJobNamePrefixMappingValue() {
        return jobNamePrefixMappingValue;
    }

    public void setJobNamePrefixMappingValue(String jobNamePrefixMappingValue) {
        this.jobNamePrefixMappingValue = jobNamePrefixMappingValue;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public List<CreateJobConfigMappingGroupVo> getJobParamMappingGroupList() {
        return jobParamMappingGroupList;
    }

    public void setJobParamMappingGroupList(List<CreateJobConfigMappingGroupVo> jobParamMappingGroupList) {
        this.jobParamMappingGroupList = jobParamMappingGroupList;
    }

    public List<CreateJobConfigMappingGroupVo> getExecuteParamMappingGroupList() {
        return executeParamMappingGroupList;
    }

    public void setExecuteParamMappingGroupList(List<CreateJobConfigMappingGroupVo> executeParamMappingGroupList) {
        this.executeParamMappingGroupList = executeParamMappingGroupList;
    }

    public CreateJobConfigMappingVo getBatchDataSourceMapping() {
        return batchDataSourceMapping;
    }

    public void setBatchDataSourceMapping(CreateJobConfigMappingVo batchDataSourceMapping) {
        this.batchDataSourceMapping = batchDataSourceMapping;
    }

    public JSONArray getFormAttributeMappingList() {
        return formAttributeMappingList;
    }

    public void setFormAttributeMappingList(JSONArray formAttributeMappingList) {
        this.formAttributeMappingList = formAttributeMappingList;
    }

    public List<CreateJobConfigMappingGroupVo> getScenarioParamMappingGroupList() {
        return scenarioParamMappingGroupList;
    }

    public void setScenarioParamMappingGroupList(List<CreateJobConfigMappingGroupVo> scenarioParamMappingGroupList) {
        this.scenarioParamMappingGroupList = scenarioParamMappingGroupList;
    }

    public String getFormTag() {
        return formTag;
    }

    public void setFormTag(String formTag) {
        this.formTag = formTag;
    }
}
