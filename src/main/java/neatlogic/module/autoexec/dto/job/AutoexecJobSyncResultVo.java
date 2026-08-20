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

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.EntityField;

import java.util.List;

public class AutoexecJobSyncResultVo {

    @EntityField(name = "term.autoexec.jobid", type = ApiParamType.LONG)
    private Long jobId;
    @EntityField(name = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.name", type = ApiParamType.STRING)
    private String jobName;
    @EntityField(name = "term.autoexec.jobstatuslabel", type = ApiParamType.STRING)
    private String status;
    @EntityField(name = "term.autoexec.jobstatusname", type = ApiParamType.STRING)
    private String statusName;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.terminal", type = ApiParamType.BOOLEAN)
    private Boolean terminal;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.success", type = ApiParamType.BOOLEAN)
    private Boolean success;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.timedout", type = ApiParamType.BOOLEAN)
    private Boolean timedOut;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.waittimems", type = ApiParamType.LONG)
    private Long waitTimeMs;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.outputtargetcount", type = ApiParamType.INTEGER)
    private Integer outputTargetCount;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.returnedoutputtargetcount", type = ApiParamType.INTEGER)
    private Integer returnedOutputTargetCount;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.outputtruncated", type = ApiParamType.BOOLEAN)
    private Boolean outputTruncated;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.outputreadsuccess", type = ApiParamType.BOOLEAN)
    private Boolean outputReadSuccess;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.outputreadmessage", type = ApiParamType.STRING)
    private String outputReadMessage;
    @EntityField(name = "nmaaja.createautoexeccombopjobsyncapi.output.nodeoutputlist", type = ApiParamType.JSONARRAY)
    private List<AutoexecJobNodeOutputVo> nodeOutputList;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getTimedOut() {
        return timedOut;
    }

    public void setTimedOut(Boolean timedOut) {
        this.timedOut = timedOut;
    }

    public Long getWaitTimeMs() {
        return waitTimeMs;
    }

    public void setWaitTimeMs(Long waitTimeMs) {
        this.waitTimeMs = waitTimeMs;
    }

    public Integer getOutputTargetCount() {
        return outputTargetCount;
    }

    public void setOutputTargetCount(Integer outputTargetCount) {
        this.outputTargetCount = outputTargetCount;
    }

    public Integer getReturnedOutputTargetCount() {
        return returnedOutputTargetCount;
    }

    public void setReturnedOutputTargetCount(Integer returnedOutputTargetCount) {
        this.returnedOutputTargetCount = returnedOutputTargetCount;
    }

    public Boolean getOutputTruncated() {
        return outputTruncated;
    }

    public void setOutputTruncated(Boolean outputTruncated) {
        this.outputTruncated = outputTruncated;
    }

    public Boolean getOutputReadSuccess() {
        return outputReadSuccess;
    }

    public void setOutputReadSuccess(Boolean outputReadSuccess) {
        this.outputReadSuccess = outputReadSuccess;
    }

    public String getOutputReadMessage() {
        return outputReadMessage;
    }

    public void setOutputReadMessage(String outputReadMessage) {
        this.outputReadMessage = outputReadMessage;
    }

    public List<AutoexecJobNodeOutputVo> getNodeOutputList() {
        return nodeOutputList;
    }

    public void setNodeOutputList(List<AutoexecJobNodeOutputVo> nodeOutputList) {
        this.nodeOutputList = nodeOutputList;
    }
}
