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

import java.util.List;

public class CreateJobConfigVo {
    private Integer rerunStepToCreateNewJob;

    private String failPolicy;

    private List<CreateJobConfigConfigVo> configList;

    public Integer getRerunStepToCreateNewJob() {
        return rerunStepToCreateNewJob;
    }

    public void setRerunStepToCreateNewJob(Integer rerunStepToCreateNewJob) {
        this.rerunStepToCreateNewJob = rerunStepToCreateNewJob;
    }

    public String getFailPolicy() {
        return failPolicy;
    }

    public void setFailPolicy(String failPolicy) {
        this.failPolicy = failPolicy;
    }

    public List<CreateJobConfigConfigVo> getConfigList() {
        return configList;
    }

    public void setConfigList(List<CreateJobConfigConfigVo> configList) {
        this.configList = configList;
    }
}
