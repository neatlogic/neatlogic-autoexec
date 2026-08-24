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

package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.job.AutoexecJobSyncInterruptedException;
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.dto.job.AutoexecCombopJobBuildResultVo;
import neatlogic.module.autoexec.dto.job.AutoexecJobNodeOutputVo;
import neatlogic.module.autoexec.dto.job.AutoexecJobSyncResultVo;
import neatlogic.module.autoexec.job.sync.AutoexecJobSyncManager;
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

@Service
public class AutoexecCombopJobSyncServiceImpl implements AutoexecCombopJobSyncService {

    private static final Logger logger = LoggerFactory.getLogger(AutoexecCombopJobSyncServiceImpl.class);
    private static final String NODE_OUTPUT_COLLECTION = "_node_output";
    private static final long POLL_INTERVAL_MS = 1000L;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;
    @Resource
    private AutoexecCombopJobCreateService autoexecCombopJobCreateService;
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private AutoexecJobSyncManager autoexecJobSyncManager;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public AutoexecJobSyncResultVo createAndWait(JSONObject paramObj, String execUserUuid) throws Exception {
        String tenantUuid = TenantContext.get().getTenantUuid();
        autoexecJobSyncManager.acquire(tenantUuid);
        Long jobId = null;
        try {
            AutoexecCombopJobBuildResultVo buildResult = autoexecCombopJobCreateService.buildJob(paramObj, execUserUuid);
            AutoexecJobVo jobVo = buildResult.getJobVo();
            // 同步模式固定本次解析到的活动版本，确保创建参数和最终作业快照来自同一版本。
            jobVo.setCombopVersionId(buildResult.getActiveVersionId());
            // 作业保存方法自行控制短事务；提交后才登记、激活并进入监听。
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
        if (Boolean.TRUE.equals(result.getTerminal())) {
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
        result.setOutputReadMessage(timedOut
                ? $.t("nmaaja.createautoexecjobfromcomboppublicapi.message.outputreadskippednotterminal")
                : StringUtils.EMPTY);
        result.setNodeOutputList(Collections.emptyList());
        return result;
    }

    /**
     * 作业终止后读取有限数量的已持久化节点输出；读取失败不改变作业结果。
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
            result.setOutputReadMessage($.t("nmaaja.createautoexecjobfromcomboppublicapi.message.outputreadfailed"));
        }
    }
}
