/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import codedriver.module.autoexec.dependency.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
public class AutoexecCombopServiceImpl implements AutoexecCombopService, IAutoexecCombopCrossoverService {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Resource
    private AutoexecProfileService autoexecProfileService;

    @Resource
    private AutoexecService autoexecService;

    @Resource
    private AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    @Override
    public void setOperableButtonList(AutoexecCombopVo autoexecCombopVo) {
        String userUuid = UserContext.get().getUserUuid(true);
        if (AuthActionChecker.check(AUTOEXEC_MODIFY.class) || Objects.equals(autoexecCombopVo.getOwner(), userUuid)) {
//            autoexecCombopVo.setViewable(1);
            autoexecCombopVo.setEditable(1);
            autoexecCombopVo.setDeletable(1);
            autoexecCombopVo.setExecutable(1);
            autoexecCombopVo.setOwnerEditable(1);
        } else {
            autoexecCombopVo.setOwnerEditable(0);
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(userUuid);
            List<String> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(autoexecCombopVo.getId(), userUuid, authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
//            if (authorityList.contains(CombopAuthorityAction.VIEW.getValue())) {
//                autoexecCombopVo.setViewable(1);
//            } else {
//                autoexecCombopVo.setViewable(0);
//            }
            if (authorityList.contains(CombopAuthorityAction.EDIT.getValue())) {
                autoexecCombopVo.setEditable(1);
                autoexecCombopVo.setDeletable(1);
//                autoexecCombopVo.setViewable(1);
            } else {
                autoexecCombopVo.setEditable(0);
                autoexecCombopVo.setDeletable(0);
            }
            if (authorityList.contains(CombopAuthorityAction.EXECUTE.getValue())) {
                autoexecCombopVo.setExecutable(1);
//                autoexecCombopVo.setViewable(1);
            } else {
                autoexecCombopVo.setExecutable(0);
            }
        }
    }

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param combopVoList 组合工具Vo对象列表
     */
    @Override
    public void setOperableButtonList(List<AutoexecCombopVo> combopVoList) {
        if (CollectionUtils.isNotEmpty(combopVoList)) {
            for (AutoexecCombopVo vo : combopVoList) {
                setOperableButtonList(vo);
            }
        }
    }

    @Override
    public boolean checkOperableButton(AutoexecCombopVo autoexecCombopVo, CombopAuthorityAction action, String userUuid) {
        AuthenticationInfoVo authenticationInfoVo;
        if (autoexecCombopVo != null) {
            if (Objects.equals(autoexecCombopVo.getOwner(), userUuid) || Objects.equals(userUuid, SystemUser.SYSTEM.getUserUuid())) {
                return true;
            } else {
                if (StringUtils.isBlank(userUuid)) {
                    userUuid = UserContext.get().getUserUuid();
                    authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
                } else {
                    authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(userUuid);
                }
                List<String> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(autoexecCombopVo.getId(), userUuid, authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
                return authorityList.contains(action.getValue());
            }
        }
        return false;
    }

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param config 组合工具Vo对象
     * @return
     */
    @Override
    public boolean verifyAutoexecCombopConfig(AutoexecCombopConfigVo config, boolean isExecuteJob) {
        if (config == null) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        boolean isNeedExecuteUser = false;
        boolean isNeedProtocol = false;
        boolean isNeedExecuteNodeConfig = false;
        Map<String, AutoexecParamVo> runtimeParamMap = new HashMap<>();
        List<AutoexecParamVo> autoexecParamVoList = config.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(autoexecParamVoList)) {
            runtimeParamMap = autoexecParamVoList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
        }
        Map<String, AutoexecParamVo> preNodeOutputParamMap = new HashMap<>();
        Map<String, String> preNodeNameMap = new HashMap<>();
        Map<String, String> preOperationNameMap = new HashMap<>();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
            if (phaseConfig == null) {
                throw new AutoexecCombopPhaseAtLeastOneOperationException();
            }
            //如果阶段存在任意"执行用户"、"协议"、"节点配置"
            AutoexecCombopExecuteConfigVo phaseExecuteConfig = phaseConfig.getExecuteConfig();
            if (phaseExecuteConfig != null) {
                if (StringUtils.isBlank(phaseExecuteConfig.getExecuteUser())) {
                    isNeedExecuteUser = true;
                }
                if (phaseExecuteConfig.getProtocolId() == null) {
                    isNeedProtocol = true;
                }
                if (phaseExecuteConfig.getExecuteNodeConfig() == null) {
                    isNeedExecuteNodeConfig = true;
                }
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                throw new AutoexecCombopPhaseAtLeastOneOperationException();
            }
            String uuid = autoexecCombopPhaseVo.getUuid();
            preNodeNameMap.put(uuid, autoexecCombopPhaseVo.getName());
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                if (autoexecCombopPhaseOperationVo == null) {
                    continue;
                }
                String phaseOperationName = autoexecCombopPhaseOperationVo.getOperationName();
                String operationLetter = autoexecCombopPhaseOperationVo.getLetter();
                if (StringUtils.isNotBlank(operationLetter)) {
                    phaseOperationName = phaseOperationName + "_" + operationLetter;
                }
                preOperationNameMap.put(autoexecCombopPhaseOperationVo.getUuid(), phaseOperationName);
                verifyAutoexecCombopPhaseOperationConfig(uuid, autoexecCombopPhaseOperationVo, preNodeOutputParamMap, runtimeParamMap, preNodeNameMap, preOperationNameMap);
                AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        String operationName = operationVo.getOperationName();
                        String letter = operationVo.getLetter();
                        if (StringUtils.isNotBlank(letter)) {
                            operationName = operationName + "_" + letter;
                        }
                        preOperationNameMap.put(operationVo.getUuid(), operationName);
                        verifyAutoexecCombopPhaseOperationConfig(uuid, operationVo, preNodeOutputParamMap, runtimeParamMap, preNodeNameMap, preOperationNameMap);
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        String operationName = operationVo.getOperationName();
                        String letter = operationVo.getLetter();
                        if (StringUtils.isNotBlank(letter)) {
                            operationName = operationName + "_" + letter;
                        }
                        preOperationNameMap.put(operationVo.getUuid(), operationName);
                        verifyAutoexecCombopPhaseOperationConfig(uuid, operationVo, preNodeOutputParamMap, runtimeParamMap, preNodeNameMap, preOperationNameMap);
                    }
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = config.getExecuteConfig();
        if (executeConfigVo != null) {
            if (Objects.equals(executeConfigVo.getWhenToSpecify(), CombopNodeSpecify.NOW.getValue())) {
                if (isExecuteJob) {
                    String executeUser = executeConfigVo.getExecuteUser();
                    if (StringUtils.isBlank(executeUser) && isNeedExecuteUser) {
                        throw new AutoexecCombopExecuteUserCannotBeEmptyException();
                    }
                    if (executeConfigVo.getProtocolId() == null && isNeedExecuteNodeConfig) {
                        throw new AutoexecCombopProtocolCannotBeEmptyException();
                    }
                }
                AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                if (executeNodeConfigVo == null) {
                    if (isNeedExecuteNodeConfig) {
                        throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
                    }
                } else {
                    List<AutoexecNodeVo> selectNodeList = executeNodeConfigVo.getSelectNodeList();
                    List<AutoexecNodeVo> inputNodeList = executeNodeConfigVo.getInputNodeList();
                    JSONObject filter = executeNodeConfigVo.getFilter();
                    if (isNeedExecuteNodeConfig && CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && MapUtils.isEmpty(filter)) {
                        throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
                    }
                }
            }
        }

        return true;
    }

    /**
     * 验证输入参数映射和自由参数映射
     *
     * @param phaseUuid                      阶段uuid
     * @param autoexecCombopPhaseOperationVo 操作信息
     * @param preNodeOutputParamMap          前置步骤输出参数
     * @param runtimeParamMap                作业参数
     * @param preNodeNameMap                 前置步骤名称
     */
    private void verifyAutoexecCombopPhaseOperationConfig(
            String phaseUuid,
            AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo,
            Map<String, AutoexecParamVo> preNodeOutputParamMap,
            Map<String, AutoexecParamVo> runtimeParamMap,
            Map<String, String> preNodeNameMap,
            Map<String, String> preOperationNameMap) {
        String phaseName = preNodeNameMap.get(phaseUuid);
        String operationUuid = autoexecCombopPhaseOperationVo.getUuid();
        String operationName = autoexecCombopPhaseOperationVo.getOperationName();
        AutoexecOperationBaseVo autoexecOperationBaseVo = autoexecService.getAutoexecOperationBaseVoByIdAndType(phaseName, autoexecCombopPhaseOperationVo, true);
        Map<String, AutoexecParamVo> inputParamMap = new HashMap<>();
        Map<String, String> inputParamNameMap = new HashMap<>();
        List<AutoexecParamVo> inputParamList = autoexecOperationBaseVo.getInputParamList();
        if (CollectionUtils.isNotEmpty(inputParamList)) {
            for (AutoexecParamVo paramVo : inputParamList) {
                inputParamMap.put(paramVo.getKey(), paramVo);
                inputParamNameMap.put(paramVo.getKey(), paramVo.getName());
            }
        }
        List<AutoexecParamVo> outputParamList = autoexecOperationBaseVo.getOutputParamList();
        if (CollectionUtils.isNotEmpty(outputParamList)) {
            for (AutoexecParamVo paramVo : outputParamList) {
                preNodeOutputParamMap.put(phaseUuid + "&&" + operationUuid + "&&" + paramVo.getKey(), paramVo);
            }
        }
        AutoexecParamVo argumentParam = autoexecOperationBaseVo.getArgument();
        AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
        if (operationConfig != null) {
            Map<String, AutoexecProfileParamVo> profileParamMap = new HashMap<>();
            Long profileId = operationConfig.getProfileId();
            if (profileId != null) {
                List<AutoexecProfileParamVo> profileParamList = autoexecProfileService.getProfileParamListById(profileId);
                if (CollectionUtils.isNotEmpty(profileParamList)) {
                    profileParamMap = profileParamList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
                }
            }
            //验证输入参数
            List<ParamMappingVo> paramMappingList = operationConfig.getParamMappingList();
            validateParam(paramMappingList, inputParamMap, null, runtimeParamMap, preNodeOutputParamMap, operationName, profileParamMap, preNodeNameMap, preOperationNameMap);
            //验证自由参数
            List<ParamMappingVo> argumentMappingList = operationConfig.getArgumentMappingList();
            validateParam(argumentMappingList, inputParamMap, argumentParam, runtimeParamMap, preNodeOutputParamMap, operationName, profileParamMap, preNodeNameMap, preOperationNameMap);
        }
        if (MapUtils.isNotEmpty(inputParamMap)) {
            Set<String> inputParamSet = new HashSet<>();
            for (String key : inputParamMap.keySet()) {
                if (inputParamMap.containsKey(key)) {
                    inputParamSet.add(inputParamNameMap.get(key) + "(" + key + ")");
                } else {
                    inputParamSet.add(key);
                }
            }
            throw new AutoexecParamMappingNotMappedException(operationName, String.join("、", inputParamSet));
        }
    }

    /**
     * 校验填写的入参值
     *
     * @param mappingList           入参|自由参
     * @param inputParamMap         输入参数
     * @param argumentParam         自由参数
     * @param runtimeParamMap       运行参数
     * @param preNodeOutputParamMap 上游节点出参
     * @param operationName         操作工具名
     */
    private void validateParam(
            List<ParamMappingVo> mappingList,
            Map<String, AutoexecParamVo> inputParamMap,
            AutoexecParamVo argumentParam,
            Map<String, AutoexecParamVo> runtimeParamMap,
            Map<String, AutoexecParamVo> preNodeOutputParamMap,
            String operationName,
            Map<String, AutoexecProfileParamVo> profileParamMap,
            Map<String, String> preNodeNameMap,
            Map<String, String> preOperationNameMap
    ) {
        if (argumentParam != null) {
            Integer argumentCount = argumentParam.getArgumentCount();
            if (argumentCount == null) {
                argumentCount = 0;
            }
            if (argumentCount != 0 && !Objects.equals(mappingList.size(), argumentCount)) {
                throw new AutoexecParamMappingArgumentCountMismatchException(operationName, argumentParam.getName(), argumentCount);
            }
        }
        if (CollectionUtils.isNotEmpty(mappingList)) {
            List<String> paramList = new ArrayList<>();
            paramList.add(ParamType.DATE.getValue());
            paramList.add(ParamType.TIME.getValue());
            paramList.add(ParamType.DATETIME.getValue());
            paramList.add(ParamType.SELECT.getValue());
            paramList.add(ParamType.RADIO.getValue());
            paramList.add(ParamType.TEXTAREA.getValue());
            paramList.add(ParamType.PHASE.getValue());
            paramList.add(ParamType.PASSWORD.getValue());
            AutoexecParamVo inputParamVo;
            String key;
            for (ParamMappingVo paramMappingVo : mappingList) {
                if (paramMappingVo == null) {
                    continue;
                }
                if (argumentParam == null) {
                    key = paramMappingVo.getKey();
                    inputParamVo = inputParamMap.remove(key);
                    if (inputParamVo == null) {
                        throw new AutoexecParamNotFoundException(operationName, key);
                    }
                } else {
                    inputParamVo = argumentParam;
                    key = "argument";
                }
                String mappingMode = paramMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ParamMappingMode.IS_EMPTY.getValue())) {
                    if (Objects.equals(inputParamVo.getIsRequired(), 1)) {
                        throw new AutoexecParamCannotBeEmptyException(operationName, key);
                    }
                    continue;
                }
                Object valueObj = paramMappingVo.getValue();
                if (Objects.equals(mappingMode, ParamMappingMode.CONSTANT.getValue())) {
                    if (valueObj == null) {
                        throw new AutoexecParamCannotBeEmptyException(operationName, key);
                    } else if (valueObj instanceof String) {
                        if (StringUtils.isBlank((String) valueObj)) {
                            throw new AutoexecParamCannotBeEmptyException(operationName, key);
                        }
                    } else if (valueObj instanceof JSONArray) {
                        if (CollectionUtils.isEmpty((JSONArray) valueObj)) {
                            throw new AutoexecParamCannotBeEmptyException(operationName, key);
                        }
                    } else if (valueObj instanceof JSONObject) {
                        if (MapUtils.isEmpty((JSONObject) valueObj)) {
                            throw new AutoexecParamCannotBeEmptyException(operationName, key);
                        }
                    }
                    continue;
                }
                String value = valueObj.toString();
                if (StringUtils.isEmpty(value)) {
                    throw new AutoexecParamMappingNotMappedException(operationName, key);
                }
                if (Objects.equals(mappingMode, ParamMappingMode.RUNTIME_PARAM.getValue())) {
                    AutoexecParamVo runtimeParamVo = runtimeParamMap.get(value);
                    if (runtimeParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, value);
                    }
                    if (!Objects.equals(runtimeParamVo.getType(), inputParamVo.getType())) {
                        if (inputParamVo.getType().equals(ParamType.TEXT.getValue())) {
                            if (!paramList.contains(runtimeParamVo.getType())) {
                                throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, value);
                            }
                        } else {
                            throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, value);
                        }
                    }

                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue())) {
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                    if (!Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM_KEY.getValue())) {
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.PROFILE.getValue())) {
                    AutoexecProfileParamVo profileParamVo = profileParamMap.get(key);
                    if (profileParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, key);
                    }
                    if (!Objects.equals(profileParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, key);
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(value);
                    if (globalParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, value);
                    }
                    if (!Objects.equals(globalParamVo.getType(), inputParamVo.getType())) {
                        if (inputParamVo.getType().equals(ParamType.TEXT.getValue())) {
                            if (!paramList.contains(globalParamVo.getType())) {
                                throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, value);
                            }
                        } else {
                            throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, value);
                        }
                    }
                } else {
                    throw new AutoexecParamMappingIncorrectException(operationName, key);
                }
            }
        }
    }

    /**
     * 将“698fb4fbd08b4bdbb7094922795f98bc&&linbq_0427&&d3126570321145858bb37b93f8d39112&&outc”转换成“阶段一.linbq_0427.outc”
     *
     * @param preNodeNameMap
     * @param value
     * @return
     */
    private String conversionPreNodeParamPath(Map<String, String> preNodeNameMap, Map<String, String> preOperationNameMap, String value) {
        String[] split = value.split("&&");
        String preNodeName = preNodeNameMap.get(split[0]);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(preNodeName)) {
            stringBuilder.append(preNodeName);
            stringBuilder.append(".");
        }
        if (split.length > 1) {
            String operationName = preOperationNameMap.get(split[1]);
            stringBuilder.append(operationName);
            stringBuilder.append(".");
        }
        if (split.length > 2) {
            stringBuilder.append(split[2]);
        }
        return stringBuilder.toString();
    }

    @Override
    public String getOperationActiveVersionScriptByOperationId(Long operationId) {
        AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
        if (scriptVersionVo == null) {
            throw new AutoexecScriptVersionHasNoActivedException(operationId.toString());
        }
        return getScriptVersionContent(scriptVersionVo);
    }

    @Override
    public void needExecuteConfig(AutoexecCombopVo autoexecCombopVo, AutoexecCombopPhaseVo autoexecCombopPhaseVo) {
        String execMode = autoexecCombopPhaseVo.getExecMode();
        if (!ExecMode.RUNNER.getValue().equals(execMode) && !ExecMode.SQL.getValue().equals(execMode)) {
            boolean needExecuteUser = autoexecCombopVo.getNeedExecuteUser();
            boolean needProtocol = autoexecCombopVo.getNeedProtocol();
            boolean needExecuteNode = autoexecCombopVo.getNeedExecuteNode();
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo == null) {
                needExecuteUser = true;
                needProtocol = true;
                needExecuteNode = true;
            }
            AutoexecCombopExecuteConfigVo executeConfigVo = autoexecCombopPhaseConfigVo.getExecuteConfig();
            if (executeConfigVo == null) {
                needExecuteUser = true;
                needProtocol = true;
                needExecuteNode = true;
            }
            if (!needProtocol) {
                Long protocolId = executeConfigVo.getProtocolId();
                if (protocolId == null) {
                    needProtocol = true;
                }
            }
            if (!needExecuteUser) {
                String executeUser = executeConfigVo.getExecuteUser();
                if (StringUtils.isBlank(executeUser)) {
                    needExecuteUser = true;
                }
            }
            if (!needExecuteNode) {
                AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                if (executeNodeConfigVo == null) {
                    needExecuteNode = true;
                } else {
                    List<String> paramList = executeNodeConfigVo.getParamList();
                    List<AutoexecNodeVo> selectNodeList = executeNodeConfigVo.getSelectNodeList();
                    List<AutoexecNodeVo> inputNodeList = executeNodeConfigVo.getInputNodeList();
                    List<Long> tagList = executeNodeConfigVo.getTagList();
                    JSONObject filter = executeNodeConfigVo.getFilter();
                    if (CollectionUtils.isEmpty(paramList) && CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && CollectionUtils.isEmpty(tagList) && MapUtils.isEmpty(filter)) {
                        needExecuteNode = true;
                    }
                }
            }
            autoexecCombopVo.setNeedExecuteUser(needExecuteUser);
            autoexecCombopVo.setNeedExecuteNode(needExecuteNode);
            autoexecCombopVo.setNeedProtocol(needProtocol);
        }
    }

    @Override
    public void saveAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo, boolean isCopy) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        Map<String, AutoexecCombopGroupVo> groupMap = new HashMap<>();
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo autoexecCombopGroupVo : combopGroupList) {
                if (isCopy) {
                    autoexecCombopGroupVo.setId(null);
                }
                groupMap.put(autoexecCombopGroupVo.getUuid(), autoexecCombopGroupVo);
            }
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo == null) {
                    continue;
                }
                if (isCopy) {
                    autoexecCombopPhaseVo.setId(null);
                }
                AutoexecCombopGroupVo autoexecCombopGroupVo = groupMap.get(autoexecCombopPhaseVo.getGroupUuid());
                if (autoexecCombopGroupVo != null) {
                    autoexecCombopPhaseVo.setGroupId(autoexecCombopGroupVo.getId());
                }
                AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                if (CollectionUtils.isNotEmpty(phaseOperationList)) {
//                    int jSort = 0;
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        if (autoexecCombopPhaseOperationVo != null) {
                            if (isCopy) {
                                autoexecCombopPhaseOperationVo.setId(null);
                            }
//                            autoexecCombopPhaseOperationVo.setSort(jSort++);
                            AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                            List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                            if (CollectionUtils.isNotEmpty(ifList)) {
                                for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                                    if (operationVo == null) {
                                        continue;
                                    }
                                    if (isCopy) {
                                        operationVo.setId(null);
                                    }
                                }
                            }
                            List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                            if (CollectionUtils.isNotEmpty(elseList)) {
                                for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                                    if (operationVo == null) {
                                        continue;
                                    }
                                    if (isCopy) {
                                        operationVo.setId(null);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVo
     */
    @Override
    public void saveDependency(AutoexecCombopVo autoexecCombopVo) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                if (autoexecCombopPhaseOperationVo == null) {
                    continue;
                }
                saveDependency(autoexecCombopVo, autoexecCombopPhaseVo, autoexecCombopPhaseOperationVo);
                AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        saveDependency(autoexecCombopVo, autoexecCombopPhaseVo, operationVo);
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        saveDependency(autoexecCombopVo, autoexecCombopPhaseVo, operationVo);
                    }
                }
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param combopPhaseVo
     * @param phaseOperationVo
     */
    private void saveDependency(AutoexecCombopVo autoexecCombopVo, AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo) {
        Long operationId = phaseOperationVo.getOperationId();
        if (operationId == null) {
            return;
        }
        {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("combopId", autoexecCombopVo.getId());
            dependencyConfig.put("combopName", autoexecCombopVo.getName());
            dependencyConfig.put("phaseId", combopPhaseVo.getId());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                DependencyManager.insert(AutoexecScript2CombopPhaseOperationDependencyHandler.class, operationId, phaseOperationVo.getId(), dependencyConfig);
            } else if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                DependencyManager.insert(AutoexecTool2CombopPhaseOperationDependencyHandler.class, operationId, phaseOperationVo.getId(), dependencyConfig);
            }
        }
        AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
        if (operationConfigVo == null) {
            return;
        }
        Long profileId = operationConfigVo.getProfileId();
        if (profileId != null) {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("combopId", autoexecCombopVo.getId());
            dependencyConfig.put("combopName", autoexecCombopVo.getName());
            dependencyConfig.put("phaseId", combopPhaseVo.getId());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            DependencyManager.insert(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getId(), dependencyConfig);
        }
        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("combopId", autoexecCombopVo.getId());
                    dependencyConfig.put("combopName", autoexecCombopVo.getName());
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    dependencyConfig.put("key", paramMappingVo.getKey());
                    dependencyConfig.put("name", paramMappingVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("combopId", autoexecCombopVo.getId());
                    dependencyConfig.put("combopName", autoexecCombopVo.getName());
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
    }

    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVo
     */
    @Override
    public void deleteDependency(AutoexecCombopVo autoexecCombopVo) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                DependencyManager.delete(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, phaseOperationVo.getId());
                DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler.class, phaseOperationVo.getId());
                DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, phaseOperationVo.getId());
                if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                    DependencyManager.delete(AutoexecScript2CombopPhaseOperationDependencyHandler.class, phaseOperationVo.getId());
                } else if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                    DependencyManager.delete(AutoexecTool2CombopPhaseOperationDependencyHandler.class, phaseOperationVo.getId());
                }
                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        DependencyManager.delete(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, operationVo.getId());
                        if (Objects.equals(operationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                            DependencyManager.delete(AutoexecScript2CombopPhaseOperationDependencyHandler.class, operationVo.getId());
                        } else if (Objects.equals(operationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                            DependencyManager.delete(AutoexecTool2CombopPhaseOperationDependencyHandler.class, operationVo.getId());
                        }
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        DependencyManager.delete(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, operationVo.getId());
                        if (Objects.equals(operationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                            DependencyManager.delete(AutoexecScript2CombopPhaseOperationDependencyHandler.class, operationVo.getId());
                        } else if (Objects.equals(operationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                            DependencyManager.delete(AutoexecTool2CombopPhaseOperationDependencyHandler.class, operationVo.getId());
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getScriptVersionContent(AutoexecScriptVersionVo scriptVersionVo) {
        List<AutoexecScriptLineVo> scriptLineVoList = autoexecScriptMapper.getLineListByVersionId(scriptVersionVo.getId());
        StringBuilder scriptSb = new StringBuilder();
        for (AutoexecScriptLineVo lineVo : scriptLineVoList) {
            scriptSb.append(lineVo.getContent() != null ? lineVo.getContent() : StringUtils.EMPTY).append("\n");
        }
        return scriptSb.toString();
    }

    @Override
    public AutoexecCombopVo getAutoexecCombopById(Long id) {
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            return null;
        }
        List<AutoexecParamVo> runtimeParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
        for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
            autoexecService.mergeConfig(autoexecParamVo);
        }
        autoexecCombopVo.setRuntimeParamList(runtimeParamList);
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        config.setRuntimeParamList(runtimeParamList);
        autoexecService.updateAutoexecCombopConfig(config);
        return autoexecCombopVo;
    }
}
