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
