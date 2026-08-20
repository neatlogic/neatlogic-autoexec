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

package neatlogic.module.autoexec.job.sync;

import neatlogic.framework.autoexec.constvalue.AutoexecTenantConfig;
import neatlogic.framework.autoexec.exception.job.AutoexecJobSyncLimitException;
import neatlogic.framework.config.ConfigManager;
import neatlogic.module.autoexec.dto.job.AutoexecJobSyncVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 管理当前应用实例内各租户正在创建或监听的同步作业请求数量。
 */
@Component
public class AutoexecJobSyncManager {

    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobSyncManager.class);
    private static final int DEFAULT_MAX_CONCURRENT = 20;
    private static final int DEFAULT_OUTPUT_MAX_TARGET_COUNT = 10;
    private static final int DEFAULT_WAIT_TIMEOUT = 300000;
    private final ConcurrentMap<String, Integer> tenantSyncCountMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<Long, AutoexecJobSyncVo>> tenantSyncJobMap = new ConcurrentHashMap<>();

    /**
     * 在创建作业前为当前租户原子预占一个同步请求名额。
     *
     * @param tenantUuid 租户 UUID
     */
    public void acquire(String tenantUuid) {
        int maxConcurrent = getMaxConcurrent();
        tenantSyncCountMap.compute(tenantUuid, (key, count) -> {
            int current = count == null ? 0 : count;
            if (current >= maxConcurrent) {
                throw new AutoexecJobSyncLimitException(maxConcurrent);
            }
            return current + 1;
        });
    }

    /**
     * 释放当前租户的一个同步请求名额。
     *
     * @param tenantUuid 租户 UUID
     * @param jobId      已创建的作业 ID，创建失败时为空
     */
    public void release(String tenantUuid, Long jobId) {
        if (jobId != null) {
            tenantSyncJobMap.computeIfPresent(tenantUuid, (key, syncJobMap) -> {
                syncJobMap.remove(jobId);
                return syncJobMap.isEmpty() ? null : syncJobMap;
            });
        }
        tenantSyncCountMap.computeIfPresent(tenantUuid, (key, count) -> count <= 1 ? null : count - 1);
    }

    /**
     * 作业创建成功后登记正在由同步请求监听的作业。
     */
    public void registerJob(String tenantUuid, Long jobId, String jobName) {
        tenantSyncJobMap.computeIfAbsent(tenantUuid, key -> new ConcurrentHashMap<>())
                .put(jobId, new AutoexecJobSyncVo(jobId, jobName));
    }

    public int getSyncCount(String tenantUuid) {
        return tenantSyncCountMap.getOrDefault(tenantUuid, 0);
    }

    public List<AutoexecJobSyncVo> getSyncJobList(String tenantUuid) {
        ConcurrentMap<Long, AutoexecJobSyncVo> syncJobMap = tenantSyncJobMap.get(tenantUuid);
        if (syncJobMap == null) {
            return new ArrayList<>();
        }
        List<AutoexecJobSyncVo> jobList = new ArrayList<>(syncJobMap.values());
        jobList.sort(Comparator.comparing(AutoexecJobSyncVo::getJobId));
        return jobList;
    }

    public int getMaxConcurrent() {
        return getPositiveConfig(AutoexecTenantConfig.AUTOEXEC_JOB_SYNC_MAX_CONCURRENT, DEFAULT_MAX_CONCURRENT);
    }

    public int getOutputMaxTargetCount() {
        return getPositiveConfig(AutoexecTenantConfig.AUTOEXEC_JOB_SYNC_OUTPUT_MAX_TARGET_COUNT, DEFAULT_OUTPUT_MAX_TARGET_COUNT);
    }

    public int getWaitTimeout() {
        return getPositiveConfig(AutoexecTenantConfig.AUTOEXEC_JOB_SYNC_WAIT_TIMEOUT, DEFAULT_WAIT_TIMEOUT);
    }

    private int getPositiveConfig(AutoexecTenantConfig tenantConfig, int defaultValue) {
        String value = ConfigManager.getConfig(tenantConfig);
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException ex) {
            logger.error("Invalid autoexec tenant config, key: {}, value: {}, use default value: {}", tenantConfig.getKey(), value, defaultValue, ex);
            return defaultValue;
        }
        logger.warn("Autoexec tenant config must be greater than 0, key: {}, value: {}, use default value: {}", tenantConfig.getKey(), value, defaultValue);
        return defaultValue;
    }
}
