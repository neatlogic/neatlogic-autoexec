/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file in the project root for license information.
 *
 */

package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.TransactionSynchronizationPool;
import neatlogic.framework.autoexec.constvalue.AutoexecOperationIndexAction;
import neatlogic.framework.autoexec.crossover.IAutoexecOperationIndexCrossoverService;
import neatlogic.framework.autoexec.dto.AutoexecOperationChangeVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Publishes committed source changes without coupling community Autoexec to an index implementation. */
@Service
public class AutoexecOperationChangeDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecOperationChangeDispatcher.class);

    public void notifyAfterCommit(String operationType, Long operationId, AutoexecOperationIndexAction action) {
        if (operationId == null) {
            return;
        }
        notifyAfterCommit(Collections.singletonList(new AutoexecOperationChangeVo(operationType, operationId, action)));
    }

    public void notifyAfterCommit(String operationType, Collection<Long> operationIdList,
                                  AutoexecOperationIndexAction action) {
        if (operationIdList == null || operationIdList.isEmpty()) {
            return;
        }
        List<AutoexecOperationChangeVo> changeList = new ArrayList<>(operationIdList.size());
        for (Long operationId : operationIdList) {
            if (operationId != null) {
                changeList.add(new AutoexecOperationChangeVo(operationType, operationId, action));
            }
        }
        notifyAfterCommit(changeList);
    }

    public void notifyAfterCommit(List<AutoexecOperationChangeVo> changeList) {
        if (changeList == null || changeList.isEmpty()) {
            return;
        }
        IAutoexecOperationIndexCrossoverService crossoverService = CrossoverServiceFactory.tryToGetApi(
                IAutoexecOperationIndexCrossoverService.class);
        if (crossoverService == null) {
            return;
        }
        List<AutoexecOperationChangeVo> immutableChangeList = Collections.unmodifiableList(new ArrayList<>(changeList));
        TransactionSynchronizationPool.execute(new NeatLogicThread("AUTOEXEC-OPERATION-INDEX-CHANGE") {
            @Override
            protected void execute() {
                try {
                    crossoverService.recordCommittedChange(immutableChangeList);
                } catch (Exception ex) {
                    // Index maintenance is best-effort and must never change the committed business result.
                    logger.error("Failed to publish committed Autoexec operation index changes", ex);
                }
            }
        });
    }
}
