/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_REVIEW;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamConfigVo;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationBaseVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptLineVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import neatlogic.module.autoexec.dependency.*;
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
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

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
            autoexecCombopVo.setViewable(1);
            autoexecCombopVo.setEditable(1);
            autoexecCombopVo.setDeletable(1);
            autoexecCombopVo.setExecutable(1);
            autoexecCombopVo.setOwnerEditable(1);
        } else {
            autoexecCombopVo.setOwnerEditable(0);
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(userUuid);
            List<String> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(autoexecCombopVo.getId(), userUuid, authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
            if (authorityList.contains(CombopAuthorityAction.VIEW.getValue())) {
                autoexecCombopVo.setViewable(1);
            } else {
                autoexecCombopVo.setViewable(0);
            }
            if (authorityList.contains(CombopAuthorityAction.EDIT.getValue())) {
                autoexecCombopVo.setEditable(1);
                autoexecCombopVo.setDeletable(1);
                autoexecCombopVo.setViewable(1);
            } else {
                autoexecCombopVo.setEditable(0);
                autoexecCombopVo.setDeletable(0);
            }
            if (authorityList.contains(CombopAuthorityAction.EXECUTE.getValue())) {
                autoexecCombopVo.setExecutable(1);
                autoexecCombopVo.setViewable(1);
            } else {
                autoexecCombopVo.setExecutable(0);
            }
        }
        if (AuthActionChecker.check(AUTOEXEC_COMBOP_REVIEW.class)) {
            autoexecCombopVo.setReviewable(1);
        } else {
            autoexecCombopVo.setReviewable(0);
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
    public boolean checkOperableButton(AutoexecCombopVo autoexecCombopVo, CombopAuthorityAction action) {
        AuthenticationInfoVo authenticationInfoVo;
        String userUuid = UserContext.get().getUserUuid();
        if (autoexecCombopVo != null) {
            if (Objects.equals(autoexecCombopVo.getOwner(), userUuid) || Objects.equals(userUuid, SystemUser.SYSTEM.getUserUuid())
                    || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_MODIFY.class.getSimpleName())) {
                return true;
            } else {
                authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
                if(authenticationInfoVo == null) {
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

    @Override
    public boolean verifyAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo config, boolean isExecuteJob) {
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
            autoexecService.validateRuntimeParamList(autoexecParamVoList);
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
            validateInputParam(paramMappingList, inputParamMap, runtimeParamMap, preNodeOutputParamMap, phaseName, operationName, profileParamMap, preNodeNameMap, preOperationNameMap);
            //验证自由参数
            AutoexecParamVo argumentParam = autoexecOperationBaseVo.getArgument();
            if (argumentParam != null) {
                List<ParamMappingVo> argumentMappingList = operationConfig.getArgumentMappingList();
                validateArgumentParam(argumentMappingList, argumentParam, runtimeParamMap, preNodeOutputParamMap, phaseName, operationName, preNodeNameMap, preOperationNameMap);
            }
        }
        if (MapUtils.isNotEmpty(inputParamMap)) {
            Set<String> inputParamSet = new HashSet<>();
            for (Map.Entry<String, AutoexecParamVo> entry : inputParamMap.entrySet()) {
                AutoexecParamVo autoexecParamVo = entry.getValue();
                if (Objects.equals(autoexecParamVo.getIsRequired(), 0)) {
                    continue;
                }
                Object defaultValue = autoexecParamVo.getDefaultValue();
                if (defaultValue != null && StringUtils.isNotBlank(defaultValue.toString())) {
                    continue;
                }
                String key = entry.getKey();
                String name = inputParamNameMap.get(key);
                if (StringUtils.isNotBlank(name)) {
                    inputParamSet.add(name + "(" + key + ")");
                } else {
                    inputParamSet.add(key);
                }
            }
            if (CollectionUtils.isNotEmpty(inputParamSet)) {
                throw new AutoexecParamMappingNotMappedException(phaseName, operationName, String.join("、", inputParamSet));
            }
        }
    }

    /**
     * 校验填写的入参映射列表
     *
     * @param mappingList           入参映射列表
     * @param inputParamMap         输入参数
     * @param runtimeParamMap       运行参数
     * @param preNodeOutputParamMap 上游节点出参
     * @param phaseName             阶段名
     * @param operationName         操作工具名
     */
    private void validateInputParam(
            List<ParamMappingVo> mappingList,
            Map<String, AutoexecParamVo> inputParamMap,
            Map<String, AutoexecParamVo> runtimeParamMap,
            Map<String, AutoexecParamVo> preNodeOutputParamMap,
            String phaseName,
            String operationName,
            Map<String, AutoexecProfileParamVo> profileParamMap,
            Map<String, String> preNodeNameMap,
            Map<String, String> preOperationNameMap
    ) {
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
            for (ParamMappingVo paramMappingVo : mappingList) {
                if (paramMappingVo == null) {
                    continue;
                }
                String key = paramMappingVo.getKey();
                AutoexecParamVo inputParamVo = inputParamMap.remove(key);
                if (inputParamVo == null) {
                    throw new AutoexecParamNotFoundException(phaseName, operationName, key);
                }
                String mappingMode = paramMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ParamMappingMode.IS_EMPTY.getValue())) {
                    if (Objects.equals(inputParamVo.getIsRequired(), 1)) {
                        throw new AutoexecParamCannotBeEmptyException(phaseName, operationName, key);
                    }
                    continue;
                }
                Object valueObj = paramMappingVo.getValue();
                if (Objects.equals(mappingMode, ParamMappingMode.CONSTANT.getValue())) {
                    if (valueObj == null) {
                        throw new AutoexecParamCannotBeEmptyException(phaseName, operationName, key);
                    } else if (valueObj instanceof String) {
                        if (StringUtils.isBlank((String) valueObj)) {
                            throw new AutoexecParamCannotBeEmptyException(phaseName, operationName, key);
                        }
                    } else if (valueObj instanceof JSONArray) {
                        if (CollectionUtils.isEmpty((JSONArray) valueObj)) {
                            throw new AutoexecParamCannotBeEmptyException(phaseName, operationName, key);
                        }
                    } else if (valueObj instanceof JSONObject) {
                        if (MapUtils.isEmpty((JSONObject) valueObj)) {
                            throw new AutoexecParamCannotBeEmptyException(phaseName, operationName, key);
                        }
                    }
                    // 文本类型参数值校验
                    if (Objects.equals(inputParamVo.getType(), ParamType.TEXT.getValue())) {
                        if (!autoexecService.validateTextTypeParamValue(inputParamVo, valueObj)) {
                            throw new AutoexecParamValueIrregularException(phaseName, operationName, inputParamVo.getName(), inputParamVo.getKey(), (String) valueObj);
                        }
                    }
                    continue;
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue())) {
                    String value = null;
                    if (valueObj instanceof JSONArray) {
                        JSONArray valueArray = (JSONArray) valueObj;
                        if (CollectionUtils.isNotEmpty(valueArray)) {
                            List<String> valueList = valueArray.toJavaList(String.class);
                            value = String.join("&&", valueList);
                        }
                    }
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                    if (Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                        continue;
                    }
                    // 文本域类型 上游节点输出参数值 文本类型
                    if (Objects.equals(inputParamVo.getType(), ParamType.TEXTAREA.getValue())) {
                        if (Objects.equals(preNodeOutputParamVo.getType(), ParamType.TEXT.getValue())) {
                            continue;
                        }
                    }
                    throw new AutoexecParamMappingTargetTypeMismatchException(phaseName, operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM_KEY.getValue())) {
                    String value = null;
                    if (valueObj instanceof JSONArray) {
                        JSONArray valueArray = (JSONArray) valueObj;
                        if (CollectionUtils.isNotEmpty(valueArray)) {
                            List<String> valueList = valueArray.toJavaList(String.class);
                            value = String.join("&&", valueList);
                        }
                    }
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                    continue;
                }

                String value = valueObj.toString();
                if (StringUtils.isEmpty(value)) {
                    throw new AutoexecParamMappingNotMappedException(phaseName, operationName, key);
                }
                if (Objects.equals(mappingMode, ParamMappingMode.RUNTIME_PARAM.getValue())) {
                    AutoexecParamVo runtimeParamVo = runtimeParamMap.get(value);
                    if (runtimeParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, value);
                    }
                    if (Objects.equals(runtimeParamVo.getType(), inputParamVo.getType())) {
                        continue;
                    }
                    if (inputParamVo.getType().equals(ParamType.TEXT.getValue())) {
                        if (paramList.contains(runtimeParamVo.getType())) {
                            continue;
                        }
                    } else if (Objects.equals(inputParamVo.getType(), ParamType.TEXTAREA.getValue())) {
                        // 文本域类型 上游节点输出参数值 文本类型
                        if (Objects.equals(runtimeParamVo.getType(), ParamType.TEXT.getValue())) {
                            continue;
                        }
                    }
                    throw new AutoexecParamMappingTargetTypeMismatchException(phaseName, operationName, key, value);
                } else if (Objects.equals(mappingMode, ParamMappingMode.PROFILE.getValue())) {
                    AutoexecProfileParamVo profileParamVo = profileParamMap.get(key);
                    if (profileParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, key);
                    }
                    if (!Objects.equals(profileParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingTargetTypeMismatchException(phaseName, operationName, key, key);
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(value);
                    if (globalParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, value);
                    }
                    if (Objects.equals(globalParamVo.getType(), inputParamVo.getType())) {
                        continue;
                    }
                    if (inputParamVo.getType().equals(ParamType.TEXT.getValue())) {
                        if (paramList.contains(globalParamVo.getType())) {
                            continue;
                        }
                    } else if (Objects.equals(inputParamVo.getType(), ParamType.TEXTAREA.getValue())) {
                        // 文本域类型 上游节点输出参数值 文本类型
                        if (Objects.equals(globalParamVo.getType(), ParamType.TEXT.getValue())) {
                            continue;
                        }
                    }
                    throw new AutoexecParamMappingTargetTypeMismatchException(phaseName, operationName, key, value);
                } else {
                    throw new AutoexecParamMappingIncorrectException(phaseName, operationName, key);
                }
            }
        }
    }

    /**
     * 校验填写的自由参数映射列表
     *
     * @param mappingList           自由参数映射列表
     * @param argumentParam         自由参数
     * @param runtimeParamMap       运行参数
     * @param preNodeOutputParamMap 上游节点出参
     * @param phaseName             阶段名
     * @param operationName         操作工具名
     */
    private void validateArgumentParam(
            List<ParamMappingVo> mappingList,
            AutoexecParamVo argumentParam,
            Map<String, AutoexecParamVo> runtimeParamMap,
            Map<String, AutoexecParamVo> preNodeOutputParamMap,
            String phaseName,
            String operationName,
            Map<String, String> preNodeNameMap,
            Map<String, String> preOperationNameMap
    ) {
        Integer argumentCount = argumentParam.getArgumentCount();
        if (argumentCount == null) {
            argumentCount = 0;
        }
        if (argumentCount != 0 && !Objects.equals(mappingList.size(), argumentCount)) {
            throw new AutoexecParamMappingArgumentCountMismatchException(phaseName, operationName, argumentParam.getName(), argumentCount);
        }
        String key = "argument";
        if (CollectionUtils.isNotEmpty(mappingList)) {
            for (ParamMappingVo paramMappingVo : mappingList) {
                if (paramMappingVo == null) {
                    continue;
                }
                String mappingMode = paramMappingVo.getMappingMode();
                Object valueObj = paramMappingVo.getValue();
                if (Objects.equals(mappingMode, ParamMappingMode.CONSTANT.getValue())) {
                    if (valueObj == null) {
                        throw new AutoexecParamCannotBeEmptyException(phaseName, operationName, key);
                    }
                    continue;
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue())) {
                    String value = null;
                    if (valueObj instanceof JSONArray) {
                        JSONArray valueArray = (JSONArray) valueObj;
                        if (CollectionUtils.isNotEmpty(valueArray)) {
                            List<String> valueList = valueArray.toJavaList(String.class);
                            value = String.join("&&", valueList);
                        }
                    }
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                    continue;
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM_KEY.getValue())) {
                    String value = null;
                    if (valueObj instanceof JSONArray) {
                        JSONArray valueArray = (JSONArray) valueObj;
                        if (CollectionUtils.isNotEmpty(valueArray)) {
                            List<String> valueList = valueArray.toJavaList(String.class);
                            value = String.join("&&", valueList);
                        }
                    }
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, conversionPreNodeParamPath(preNodeNameMap, preOperationNameMap, value));
                    }
                    continue;
                }

                String value = valueObj.toString();
                if (StringUtils.isEmpty(value)) {
                    throw new AutoexecParamMappingNotMappedException(phaseName, operationName, key);
                }
                if (Objects.equals(mappingMode, ParamMappingMode.RUNTIME_PARAM.getValue())) {
                    AutoexecParamVo runtimeParamVo = runtimeParamMap.get(value);
                    if (runtimeParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, value);
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(value);
                    if (globalParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(phaseName, operationName, key, value);
                    }
                } else {
                    throw new AutoexecParamMappingIncorrectException(phaseName, operationName, key);
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
            boolean needRoundCount = autoexecCombopVo.getNeedRoundCount();
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo == null) {
                needExecuteUser = true;
                needProtocol = true;
                needExecuteNode = true;
                needRoundCount = true;
            }
            AutoexecCombopExecuteConfigVo executeConfigVo = autoexecCombopPhaseConfigVo.getExecuteConfig();
            if (executeConfigVo == null) {
                needExecuteUser = true;
                needProtocol = true;
                needExecuteNode = true;
                needRoundCount = true;
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
                    List<String> preOutputList = executeNodeConfigVo.getPreOutputList();
                    JSONObject filter = executeNodeConfigVo.getFilter();
                    if (CollectionUtils.isEmpty(paramList) && CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && CollectionUtils.isEmpty(preOutputList) && MapUtils.isEmpty(filter)) {
                        needExecuteNode = true;
                    }
                }
            }
            if (!needRoundCount) {
                if (executeConfigVo.getRoundCount() == null) {
                    needRoundCount = true;
                }
            }
            autoexecCombopVo.setNeedExecuteUser(needExecuteUser);
            autoexecCombopVo.setNeedExecuteNode(needExecuteNode);
            autoexecCombopVo.setNeedProtocol(needProtocol);
            autoexecCombopVo.setNeedRoundCount(needRoundCount);
        }
    }

    @Override
    public void needExecuteConfig(AutoexecCombopVersionVo autoexecCombopVersionVo, AutoexecCombopPhaseVo autoexecCombopPhaseVo) {
        String execMode = autoexecCombopPhaseVo.getExecMode();
        if (!ExecMode.RUNNER.getValue().equals(execMode) && !ExecMode.SQL.getValue().equals(execMode)) {
            boolean needExecuteUser = autoexecCombopVersionVo.getNeedExecuteUser();
            boolean needProtocol = autoexecCombopVersionVo.getNeedProtocol();
            boolean needExecuteNode = autoexecCombopVersionVo.getNeedExecuteNode();
            boolean needRoundCount = autoexecCombopVersionVo.getNeedRoundCount();
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo == null) {
                needExecuteUser = true;
                needProtocol = true;
                needExecuteNode = true;
                needRoundCount = true;
            }
            AutoexecCombopExecuteConfigVo executeConfigVo = autoexecCombopPhaseConfigVo.getExecuteConfig();
            if (executeConfigVo == null) {
                needExecuteUser = true;
                needProtocol = true;
                needExecuteNode = true;
                needRoundCount = true;
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
                    List<String> preOutputList = executeNodeConfigVo.getPreOutputList();
                    JSONObject filter = executeNodeConfigVo.getFilter();
                    if (CollectionUtils.isEmpty(paramList) && CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && CollectionUtils.isEmpty(preOutputList) && MapUtils.isEmpty(filter)) {
                        needExecuteNode = true;
                    }
                }
            }
            if (!needRoundCount) {
                if (executeConfigVo.getRoundCount() == null) {
                    needRoundCount = true;
                }
            }
            autoexecCombopVersionVo.setNeedExecuteUser(needExecuteUser);
            autoexecCombopVersionVo.setNeedExecuteNode(needExecuteNode);
            autoexecCombopVersionVo.setNeedProtocol(needProtocol);
            autoexecCombopVersionVo.setNeedRoundCount(needRoundCount);
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

    @Override
    public void setAutoexecCombopPhaseGroupId(AutoexecCombopVersionConfigVo config) {
        Map<String, AutoexecCombopGroupVo> groupMap = new HashMap<>();
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo autoexecCombopGroupVo : combopGroupList) {
                groupMap.put(autoexecCombopGroupVo.getUuid(), autoexecCombopGroupVo);
            }
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo == null) {
                    continue;
                }
                AutoexecCombopGroupVo autoexecCombopGroupVo = groupMap.get(autoexecCombopPhaseVo.getGroupUuid());
                if (autoexecCombopGroupVo != null) {
                    autoexecCombopPhaseVo.setGroupId(autoexecCombopGroupVo.getId());
                }
            }
        }
    }

    @Override
    public void resetIdAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo config) {
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo autoexecCombopGroupVo : combopGroupList) {
                if (autoexecCombopGroupVo == null) {
                    continue;
                }
                autoexecCombopGroupVo.setId(null);
            }
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo == null) {
                    continue;
                }
                autoexecCombopPhaseVo.setId(null);
                AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        if (autoexecCombopPhaseOperationVo != null) {
                            autoexecCombopPhaseOperationVo.setId(null);
                            AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                            List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                            if (CollectionUtils.isNotEmpty(ifList)) {
                                for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                                    if (operationVo == null) {
                                        continue;
                                    }
                                    operationVo.setId(null);
                                }
                            }
                            List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                            if (CollectionUtils.isNotEmpty(elseList)) {
                                for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                                    if (operationVo == null) {
                                        continue;
                                    }
                                    operationVo.setId(null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void prepareAutoexecCombopVersionConfig(AutoexecCombopVersionConfigVo config, boolean isCopy) {
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
        List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            for (AutoexecParamVo paramVo : runtimeParamList) {
                if (paramVo == null) {
                    continue;
                }
                String type = paramVo.getType();
                ParamType paramType = ParamType.getParamType(type);
                Object value = paramVo.getDefaultValue();
                // 如果默认值不以"RC4:"开头，说明修改了密码，则重新加密
                if (paramType == ParamType.PASSWORD && value != null) {
                    paramVo.setDefaultValue(RC4Util.encrypt((String) value));
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

        JSONObject dependencyConfig = new JSONObject();
        dependencyConfig.put("combopId", autoexecCombopVo.getId());
        dependencyConfig.put("combopName", autoexecCombopVo.getName());

        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
                dependencyConfig.put("scenarioId", scenarioVo.getScenarioId());
                dependencyConfig.put("scenarioName", scenarioVo.getScenarioName());
                DependencyManager.insert(AutoexecScenarioCombopDependencyHandler.class, scenarioVo.getScenarioId(), autoexecCombopVo.getId(), dependencyConfig);
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param autoexecCombopVersionVo
     */
    @Override
    public void saveDependency(AutoexecCombopVersionVo autoexecCombopVersionVo) {
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
        if (config == null) {
            return;
        }

        JSONObject dependencyConfig = new JSONObject();
        dependencyConfig.put("combopId", autoexecCombopVersionVo.getId());
        dependencyConfig.put("combopName", autoexecCombopVersionVo.getName());

//        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
//        if (CollectionUtils.isNotEmpty(scenarioList)) {
//            for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
//                dependencyConfig.put("scenarioId", scenarioVo.getScenarioId());
//                dependencyConfig.put("scenarioName", scenarioVo.getScenarioName());
//                DependencyManager.insert(AutoexecScenarioCombopDependencyHandler.class, scenarioVo.getScenarioId(), autoexecCombopVersionVo.getId(), dependencyConfig);
//            }
//        }

        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
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
                    saveDependency(autoexecCombopVersionVo, autoexecCombopPhaseVo, autoexecCombopPhaseOperationVo);
                    AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                    if (CollectionUtils.isNotEmpty(ifList)) {
                        for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                            if (operationVo == null) {
                                continue;
                            }
                            saveDependency(autoexecCombopVersionVo, autoexecCombopPhaseVo, operationVo);
                        }
                    }
                    List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                    if (CollectionUtils.isNotEmpty(elseList)) {
                        for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                            if (operationVo == null) {
                                continue;
                            }
                            saveDependency(autoexecCombopVersionVo, autoexecCombopPhaseVo, operationVo);
                        }
                    }
                }
            }
        }
        List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            for (AutoexecParamVo paramVo : runtimeParamList) {
                if (paramVo == null) {
                    continue;
                }
                String key = paramVo.getKey();
                String type = paramVo.getType();
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == ParamType.SELECT || paramType == ParamType.MULTISELECT || paramType == ParamType.CHECKBOX || paramType == ParamType.RADIO) {
                    AutoexecParamConfigVo paramConfig = paramVo.getConfig();
                    if (paramConfig != null) {
                        String matrixUuid = paramConfig.getMatrixUuid();
                        if (StringUtils.isNotBlank(matrixUuid)) {
                            JSONArray callers = new JSONArray();
                            callers.add(autoexecCombopVersionVo.getCombopId());
                            callers.add(key);
                            DependencyManager.insert(MatrixAutoexecCombopParamDependencyHandler.class, matrixUuid, callers);
                        }
                    }
                }

            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系、组合工具对场景的引用
     *
     * @param combopPhaseVo
     * @param phaseOperationVo
     */
    private void saveDependency(AutoexecCombopVersionVo autoexecCombopVersionVo, AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo) {
        Long operationId = phaseOperationVo.getOperationId();
        if (operationId == null) {
            return;
        }
        {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("combopId", autoexecCombopVersionVo.getId());
            dependencyConfig.put("combopName", autoexecCombopVersionVo.getName());
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
            dependencyConfig.put("combopId", autoexecCombopVersionVo.getId());
            dependencyConfig.put("combopName", autoexecCombopVersionVo.getName());
            dependencyConfig.put("phaseId", combopPhaseVo.getId());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            DependencyManager.insert(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getId(), dependencyConfig);
        }
        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("combopId", autoexecCombopVersionVo.getId());
                    dependencyConfig.put("combopName", autoexecCombopVersionVo.getName());
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
                    dependencyConfig.put("combopId", autoexecCombopVersionVo.getId());
                    dependencyConfig.put("combopName", autoexecCombopVersionVo.getName());
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
    }

    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系、组合工具对场景的引用
     *
     * @param autoexecCombopVo
     */
    @Override
    public void deleteDependency(AutoexecCombopVo autoexecCombopVo) {
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        if (config == null) {
            return;
        }

        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
                DependencyManager.delete(AutoexecScenarioCombopDependencyHandler.class, autoexecCombopVo.getId());
        }
    }
    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系、组合工具对场景的引用
     *
     * @param autoexecCombopVersionVo
     */
    @Override
    public void deleteDependency(AutoexecCombopVersionVo autoexecCombopVersionVo) {
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
        if (config == null) {
            return;
        }

//        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
//        if (CollectionUtils.isNotEmpty(scenarioList)) {
//                DependencyManager.delete(AutoexecScenarioCombopDependencyHandler.class, autoexecCombopVersionVo.getId());
//        }

        if (CollectionUtils.isNotEmpty(config.getRuntimeParamList())) {
            DependencyManager.delete(MatrixAutoexecCombopParamDependencyHandler.class, autoexecCombopVersionVo.getCombopId());
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

    @Override
    public AutoexecCombopVersionVo getAutoexecCombopVersionById(Long id) {
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(id);
        if (autoexecCombopVersionVo == null) {
            return null;
        }
//        List<AutoexecParamVo> runtimeParamList = autoexecCombopVersionMapper.getAutoexecCombopVersionParamListByCombopVersionId(id);
//        for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
//            autoexecService.mergeConfig(autoexecParamVo);
//        }
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
//        config.setRuntimeParamList(runtimeParamList);
        autoexecService.updateAutoexecCombopVersionConfig(config);
        return autoexecCombopVersionVo;
    }

    @Override
    public void updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(AutoexecCombopConfigVo config) {
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                if (combopPhaseVo == null) {
                    continue;
                }
                AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
                if (phaseConfig == null) {
                    continue;
                }
                AutoexecCombopExecuteConfigVo executeConfig = phaseConfig.getExecuteConfig();
                if (executeConfig != null) {
                    updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(executeConfig);
                }
            }
        }
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo combopGroupVo : combopGroupList) {
                if (combopGroupVo == null) {
                    continue;
                }
                AutoexecCombopGroupConfigVo groupConfig = combopGroupVo.getConfig();
                if (groupConfig == null) {
                    continue;
                }
                AutoexecCombopExecuteConfigVo executeConfig = groupConfig.getExecuteConfig();
                if (executeConfig != null) {
                    updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(executeConfig);
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = config.getExecuteConfig();
        if (executeConfigVo != null) {
            updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(executeConfigVo);
        }
    }

    @Override
    public void updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(AutoexecCombopVersionConfigVo config) {
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                if (combopPhaseVo == null) {
                    continue;
                }
                AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
                if (phaseConfig == null) {
                    continue;
                }
                AutoexecCombopExecuteConfigVo executeConfig = phaseConfig.getExecuteConfig();
                if (executeConfig != null) {
                    updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(executeConfig);
                }
            }
        }
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo combopGroupVo : combopGroupList) {
                if (combopGroupVo == null) {
                    continue;
                }
                AutoexecCombopGroupConfigVo groupConfig = combopGroupVo.getConfig();
                if (groupConfig == null) {
                    continue;
                }
                AutoexecCombopExecuteConfigVo executeConfig = groupConfig.getExecuteConfig();
                if (executeConfig != null) {
                    updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(executeConfig);
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = config.getExecuteConfig();
        if (executeConfigVo != null) {
            updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(executeConfigVo);
        }
    }

    /**
     * 根据protocolId补充protocol字段和protocolPort字段值
     * @param config 执行目标config
     */
    private void updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(AutoexecCombopExecuteConfigVo config) {
        Long protocolId = config.getProtocolId();
        if (protocolId != null) {
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountProtocolVo protocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolId(protocolId);
            if (protocolVo != null) {
                config.setProtocol(protocolVo.getName());
                config.setProtocolPort(protocolVo.getPort());
            } else {
                config.setProtocol(null);
                config.setProtocolPort(null);
            }
        } else {
            config.setProtocol(null);
            config.setProtocolPort(null);
        }
    }
}
