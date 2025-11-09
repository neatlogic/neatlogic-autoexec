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

package neatlogic.module.autoexec.operationauth.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskVo;
import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.operationauth.core.OperationAuthHandlerBase;
import neatlogic.framework.process.operationauth.core.PredicateResult;
import neatlogic.framework.process.operationauth.core.TernaryPredicate;
import neatlogic.module.autoexec.operationauth.exception.ProcessTaskAutoexecHandlerNotEnableOperateException;
import neatlogic.module.autoexec.process.constvalue.CreateJobProcessStepHandlerType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linbq
 * @since 2021/9/8 17:48
 **/
@Component
public class AutoexecOperateHandler extends OperationAuthHandlerBase {

    private final Map<IOperationType, TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<IOperationType, ProcessTaskPermissionDeniedException>>, JSONObject>> operationBiPredicateMap = new HashMap<>();

    @PostConstruct
    public void init() {
//        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_START,
//                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap) -> {
//                    Long id = processTaskStepVo.getId();
//                    ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_START;
//                    //1.提示“自动化节点不支持'开始'操作”；
//                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
//                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType));
//                    return false;
//                });
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_RETREAT,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    IOperationType operationType = ProcessTaskStepOperationType.STEP_RETREAT;
                    //1.提示“自动化节点不支持'撤回'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType.getText()));
                    return PredicateResult.DENY;
                });
//        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_ACCEPT,
//                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap) -> {
//                    Long id = processTaskStepVo.getId();
//                    ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_ACCEPT;
//                    //1.提示“自动化节点不支持'开始'操作”；
//                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
//                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType));
//                    return false;
//                });
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_WORK,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    IOperationType operationType = ProcessTaskStepOperationType.STEP_WORK;
                    //1.提示“自动化节点不支持'处理'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType.getText()));
                    return PredicateResult.DENY;
                });
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_COMMENT,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    IOperationType operationType = ProcessTaskStepOperationType.STEP_COMMENT;
                    //1.提示“自动化节点不支持'回复'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType.getText()));
                    return PredicateResult.DENY;
                });

//        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_COMPLETE,
//                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
//                    Long id = processTaskStepVo.getId();
//                    List<Long> jobIdList = autoexecJobMapper.getJobIdListByInvokeId(id);
//                    if (CollectionUtils.isEmpty(jobIdList)) {
//                        return true;
//                    }
//                    int running = 0;
//                    List<AutoexecJobVo> autoexecJobList = autoexecJobMapper.getJobListByIdList(jobIdList);
//                    for (AutoexecJobVo autoexecJobVo : autoexecJobList) {
//                        if (JobStatus.isRunningStatus(autoexecJobVo.getStatus())) {
//                            running++;
//                        }
//                    }
//
//                    if (running == 0) {
//                        return true;
//                    }
//                    ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_COMPLETE;
//
//                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
//                            .put(operationType, new ProcessTaskAutoexecJobRunningException());
//                    return false;
//                });
    }

    @Override
    public Map<IOperationType, TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<IOperationType, ProcessTaskPermissionDeniedException>>, JSONObject>> getOperationBiPredicateMap() {
        return operationBiPredicateMap;
    }

    @Override
    public String getHandler() {
        return CreateJobProcessStepHandlerType.CREATE_JOB.getHandler();
    }
}
