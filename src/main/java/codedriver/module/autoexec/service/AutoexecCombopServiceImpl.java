/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: linbq
 * @since: 2021/4/15 16:13
 **/
@Service
public class AutoexecCombopServiceImpl implements AutoexecCombopService {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private TeamMapper teamMapper;

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    @Override
    public void setOperableButtonList(AutoexecCombopVo autoexecCombopVo) {
        String userUuid = UserContext.get().getUserUuid(true);
        if (Objects.equals(autoexecCombopVo.getOwner(), userUuid)) {
            autoexecCombopVo.setEditable(1);
            autoexecCombopVo.setDeletable(1);
            autoexecCombopVo.setExecutable(1);
            autoexecCombopVo.setOwnerEditable(1);
        } else {
            autoexecCombopVo.setOwnerEditable(0);
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
            List<String> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(autoexecCombopVo.getId(), userUuid, teamUuidList, roleUuidList);
            if (authorityList.contains(CombopAuthorityAction.EDIT.getValue())) {
                autoexecCombopVo.setEditable(1);
                autoexecCombopVo.setDeletable(1);
            } else {
                autoexecCombopVo.setEditable(0);
                autoexecCombopVo.setDeletable(0);
            }
            if (authorityList.contains(CombopAuthorityAction.EXECUTE.getValue())) {
                autoexecCombopVo.setExecutable(1);
            } else {
                autoexecCombopVo.setExecutable(0);
            }
        }
    }

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param autoexecCombopVo 组合工具Vo对象
     * @return
     */
    @Override
    public boolean verifyAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        Map<String, AutoexecCombopParamVo> runtimeParamMap = new HashMap<>();
        List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(autoexecCombopVo.getId());
        if (CollectionUtils.isNotEmpty(autoexecCombopParamVoList)) {
            runtimeParamMap = autoexecCombopParamVoList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
        }
        Map<String, AutoexecScriptVersionParamVo> runnerPreNodeOutputParamMap = new HashMap<>();
        Map<String, AutoexecScriptVersionParamVo> targetPreNodeOutputParamMap = new HashMap<>();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
            if (phaseConfig == null) {
                throw new AutoexecCombopPhaseAtLeastOneOperationException();
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                throw new AutoexecCombopPhaseAtLeastOneOperationException();
            }
            String name = autoexecCombopPhaseVo.getName();
            String execMode = autoexecCombopPhaseVo.getExecMode();
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                if (autoexecCombopPhaseOperationVo == null) {
                    continue;
                }
                Map<String, AutoexecScriptVersionParamVo> inputParamMap = new HashMap<>();
                Long operationId = autoexecCombopPhaseOperationVo.getOperationId();
                if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                    if (autoexecScriptMapper.checkScriptIsExistsById(operationId) == 0) {
                        throw new AutoexecScriptNotFoundException(operationId);
                    }
                    List<AutoexecScriptVersionParamVo> autoexecScriptVersionParamVoList = autoexecScriptMapper.getParamListByScriptId(operationId);
                    for (AutoexecScriptVersionParamVo paramVo : autoexecScriptVersionParamVoList) {
                        if (Objects.equals(paramVo.getMode(), ParamMode.INPUT.getValue())) {
                            inputParamMap.put(paramVo.getKey(), paramVo);
                        } else if (Objects.equals(paramVo.getMode(), ParamMode.OUTPUT.getValue())) {
                            if (Objects.equals(ExecMode.RUNNER.getValue(), execMode)) {
                                runnerPreNodeOutputParamMap.put(name + "&&" + operationId + "&&" + paramVo.getKey(), paramVo);
                            } else if (Objects.equals(ExecMode.TARGET.getValue(), execMode)) {
                                targetPreNodeOutputParamMap.put(name + "&&" + operationId + "&&" + paramVo.getKey(), paramVo);
                            }
                        }
                    }
                } else {
                    //TODO linbq 工具暂时不实现
                }

                AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                if (operationConfig != null) {
                    List<ParamMappingVo> paramMappingList = operationConfig.getParamMappingList();
                    if (CollectionUtils.isNotEmpty(paramMappingList)) {
                        for (ParamMappingVo paramMappingVo : paramMappingList) {
                            if (paramMappingVo == null) {
                                continue;
                            }
                            String key = paramMappingVo.getKey();
                            AutoexecScriptVersionParamVo inputParamVo = inputParamMap.remove(key);
                            if (inputParamVo == null) {
                                throw new AutoexecParamNotFoundException(key);
                            }
                            String mappingMode = paramMappingVo.getMappingMode();
                            if (Objects.equals(mappingMode, ParamMappingMode.IS_EMPTY.getValue()) || Objects.equals(mappingMode, ParamMappingMode.CONSTANT.getValue())) {
                                if (Objects.equals(inputParamVo.getIsRequired(), 1)) {
                                    throw new AutoexecParamMappingIncorrectException(key);
                                }
                                continue;
                            }
                            String value = (String) paramMappingVo.getValue();
                            if (StringUtils.isEmpty(value)) {
                                throw new AutoexecParamMappingIncorrectException(key);
                            }
                            if (Objects.equals(mappingMode, ParamMappingMode.RUNTIME_PARAM.getValue())) {
                                AutoexecCombopParamVo runtimeParamVo = runtimeParamMap.get(value);
                                if (runtimeParamVo == null) {
                                    throw new AutoexecParamMappingIncorrectException(key);
                                }
                                if (!Objects.equals(runtimeParamVo.getType(), inputParamVo.getType())) {
                                    throw new AutoexecParamMappingIncorrectException(key);
                                }
                            } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue())) {
                                if (Objects.equals(ExecMode.RUNNER.getValue(), execMode)) {
                                    AutoexecScriptVersionParamVo localPreNodeOutputParamVo = runnerPreNodeOutputParamMap.get(value);
                                    if (localPreNodeOutputParamVo == null) {
                                        throw new AutoexecParamMappingIncorrectException(key);
                                    }
                                    if (!Objects.equals(localPreNodeOutputParamVo.getType(), inputParamVo.getType())) {
                                        throw new AutoexecParamMappingIncorrectException(key);
                                    }
                                } else if (Objects.equals(ExecMode.RUNNER_TARGET.getValue(), execMode)) {
                                    AutoexecScriptVersionParamVo preNodeOutputParamVo = targetPreNodeOutputParamMap.get(value);
                                    if (preNodeOutputParamVo == null) {
                                        preNodeOutputParamVo = runnerPreNodeOutputParamMap.get(value);
                                        if (preNodeOutputParamVo == null) {
                                            throw new AutoexecParamMappingIncorrectException(key);
                                        }
                                    }
                                    if (!Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                                        throw new AutoexecParamMappingIncorrectException(key);
                                    }
                                }
                            } else {
                                throw new AutoexecParamMappingIncorrectException(key);
                            }
                        }
                    }
                }
                if (MapUtils.isNotEmpty(inputParamMap)) {
                    throw new AutoexecParamMappingNotMappedException(String.join("、", inputParamMap.keySet()));
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = config.getExecuteConfig();
        if (executeConfigVo == null) {
            throw new AutoexecCombopExecuteUserCannotBeEmptyException();
        }
        String executeUser = executeConfigVo.getExecuteUser();
        if (StringUtils.isBlank(executeUser)) {
            throw new AutoexecCombopExecuteUserCannotBeEmptyException();
        }
        // TODO 验证执行用户是否存在
        return true;
    }
}
