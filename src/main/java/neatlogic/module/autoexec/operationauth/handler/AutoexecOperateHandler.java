/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.operationauth.handler;

import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskVo;
import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;
import neatlogic.framework.process.operationauth.core.OperationAuthHandlerBase;
import neatlogic.framework.process.operationauth.core.TernaryPredicate;
import neatlogic.module.autoexec.operationauth.exception.ProcessTaskAutoexecHandlerNotEnableOperateException;
import com.alibaba.fastjson.JSONObject;
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

    private final Map<ProcessTaskOperationType, TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<ProcessTaskOperationType, ProcessTaskPermissionDeniedException>>, JSONObject>> operationBiPredicateMap = new HashMap<>();

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
        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_RETREAT,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_RETREAT;
                    //1.提示“自动化节点不支持'撤回'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType));
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
        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_WORK,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_WORK;
                    //1.提示“自动化节点不支持'处理'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType));
                    return false;
                });
        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_COMMENT,
                (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                    Long id = processTaskStepVo.getId();
                    ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_COMMENT;
                    //1.提示“自动化节点不支持'回复'操作”；
                    operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                            .put(operationType, new ProcessTaskAutoexecHandlerNotEnableOperateException(operationType));
                    return false;
                });
    }
    @Override
    public Map<ProcessTaskOperationType, TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<ProcessTaskOperationType, ProcessTaskPermissionDeniedException>>, JSONObject>> getOperationBiPredicateMap() {
        return operationBiPredicateMap;
    }

    @Override
    public String getHandler() {
        return AutoexecOperationAuthHandlerType.AUTOEXEC.getValue();
    }
}
