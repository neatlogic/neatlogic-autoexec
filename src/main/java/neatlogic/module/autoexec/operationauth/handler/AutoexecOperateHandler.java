/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.operationauth.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskVo;
import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;
import neatlogic.framework.process.operationauth.core.OperationAuthHandlerBase;
import neatlogic.framework.process.operationauth.core.TernaryPredicate;
import neatlogic.module.autoexec.operationauth.exception.ProcessTaskAutoexecHandlerNotEnableOperateException;
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
                    return false;
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
                    return false;
                });
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_COMMENT,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    IOperationType operationType = ProcessTaskStepOperationType.STEP_COMMENT;
                    //1.提示“自动化节点不支持'回复'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType.getText()));
                    return false;
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
        return AutoexecOperationAuthHandlerType.AUTOEXEC.getValue();
    }
}
