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

    private String type;

    private String formAttributeUuid;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormAttributeUuid() {
        return formAttributeUuid;
    }

    public void setFormAttributeUuid(String formAttributeUuid) {
        this.formAttributeUuid = formAttributeUuid;
    }
}
