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

package neatlogic.module.autoexec.dto.job;

import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;

/**
 * 根据组合工具活动版本构建作业的内部结果。
 */
public class AutoexecCombopJobBuildResultVo {

    private AutoexecJobVo jobVo;
    private Long activeVersionId;

    public AutoexecCombopJobBuildResultVo(AutoexecJobVo jobVo, Long activeVersionId) {
        this.jobVo = jobVo;
        this.activeVersionId = activeVersionId;
    }

    public AutoexecJobVo getJobVo() {
        return jobVo;
    }

    public void setJobVo(AutoexecJobVo jobVo) {
        this.jobVo = jobVo;
    }

    public Long getActiveVersionId() {
        return activeVersionId;
    }

    public void setActiveVersionId(Long activeVersionId) {
        this.activeVersionId = activeVersionId;
    }
}
