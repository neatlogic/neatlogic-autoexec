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
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import codedriver.module.autoexec.dependency.AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler;
import codedriver.module.autoexec.dependency.AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler;
import codedriver.module.autoexec.dependency.AutoexecProfile2CombopPhaseOperationDependencyHandler;
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
    public boolean checkOperableButton(AutoexecCombopVo autoexecCombopVo, CombopAuthorityAction action) {
        if (autoexecCombopVo != null) {
            String userUuid = UserContext.get().getUserUuid();
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            if (Objects.equals(autoexecCombopVo.getOwner(), userUuid)) {
                return true;
            } else {
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
     * @param autoexecCombopVo 组合工具Vo对象
     * @return
     */
    @Override
    public boolean verifyAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo, boolean isExecuteJob) {
        boolean isNeedExecuteUser = false;
        boolean isNeedProtocol = false;
        boolean isNeedExecuteNodeConfig = false;
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
        Map<String, AutoexecParamVo> preNodeOutputParamMap = new HashMap<>();
        Map<String, String> preNodeNameMap = new HashMap<>();
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
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                if (autoexecCombopPhaseOperationVo == null) {
                    continue;
                }
                Long id = autoexecCombopPhaseOperationVo.getId();
                String operationUuid = autoexecCombopPhaseOperationVo.getUuid();
                String operationName = autoexecCombopPhaseOperationVo.getName();
                List<? extends AutoexecParamVo> autoexecParamVoList = new ArrayList<>();
                Map<String, AutoexecParamVo> inputParamMap = new HashMap<>();
                Map<String, String> inputParamNameMap = new HashMap<>();
                AutoexecParamVo argumentParam = null;
                if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                    AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(id);
                    if (autoexecScriptVersionVo == null) {
                        throw new AutoexecScriptNotFoundException(id);
                    }
                    autoexecParamVoList = autoexecScriptMapper.getParamListByScriptId(id);
                    argumentParam = autoexecScriptMapper.getArgumentByVersionId(autoexecScriptVersionVo.getId());
                } else {
                    AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(id);
                    if (autoexecToolVo == null) {
                        throw new AutoexecToolNotFoundException(id);
                    }
                    JSONObject toolConfig = autoexecToolVo.getConfig();
                    if (MapUtils.isNotEmpty(toolConfig)) {
                        JSONArray paramArray = toolConfig.getJSONArray("paramList");
                        if (CollectionUtils.isNotEmpty(paramArray)) {
                            autoexecParamVoList = paramArray.toJavaList(AutoexecParamVo.class);
                        }
                        JSONObject argumentJson = toolConfig.getJSONObject("argument");
                        if (MapUtils.isNotEmpty(argumentJson)) {
                            argumentParam = argumentJson.toJavaObject(AutoexecParamVo.class);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(autoexecParamVoList)) {
                    for (AutoexecParamVo paramVo : autoexecParamVoList) {
                        if (Objects.equals(paramVo.getMode(), ParamMode.INPUT.getValue())) {
                            inputParamMap.put(paramVo.getKey(), paramVo);
                            inputParamNameMap.put(paramVo.getKey(), paramVo.getName());
                        } else if (Objects.equals(paramVo.getMode(), ParamMode.OUTPUT.getValue())) {
                            preNodeOutputParamMap.put(uuid + "&&" + operationName + "&&" + operationUuid + "&&" + paramVo.getKey(), paramVo);
                            preNodeNameMap.put(uuid, autoexecCombopPhaseVo.getName());
                        }
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
                    validateParam(paramMappingList, inputParamMap, null, runtimeParamMap, preNodeOutputParamMap, operationName, profileParamMap, preNodeNameMap);
                    //验证自由参数
                    List<ParamMappingVo> argumentMappingList = operationConfig.getArgumentMappingList();
                    validateParam(argumentMappingList, inputParamMap, argumentParam, runtimeParamMap, preNodeOutputParamMap, operationName, profileParamMap, preNodeNameMap);
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
            Map<String, AutoexecCombopParamVo> runtimeParamMap,
            Map<String, AutoexecParamVo> preNodeOutputParamMap,
            String operationName,
            Map<String, AutoexecProfileParamVo> profileParamMap,
            Map<String, String> preNodeNameMap
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
                    AutoexecCombopParamVo runtimeParamVo = runtimeParamMap.get(value);
                    if (runtimeParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, value);
                    }
                    if (!Objects.equals(runtimeParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, value);
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM.getValue())) {
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, value));
                    }
                    if (!Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, value));
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM_KEY.getValue())) {
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingTargetNotFoundException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, value));
                    }
                    if (!Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, conversionPreNodeParamPath(preNodeNameMap, value));
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
                        throw new AutoexecParamMappingTargetTypeMismatchException(operationName, key, value);
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
    private String conversionPreNodeParamPath(Map<String, String> preNodeNameMap, String value) {
        System.out.println(value);
        String[] split = value.split("&&");
        String preNodeName = preNodeNameMap.get(split[0]);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(preNodeName)) {
            stringBuilder.append(preNodeName);
            stringBuilder.append(".");
        }
        if (split.length > 1) {
            stringBuilder.append(split[1]);
            stringBuilder.append(".");
        }
        if (split.length > 3) {
            stringBuilder.append(split[3]);
        }
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    @Override
    public String getOperationActiveVersionScriptByOperationId(Long operationId) {
        AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
        if (scriptVersionVo == null) {
            throw new AutoexecScriptVersionHasNoActivedException(operationId.toString());
        }
        return getOperationActiveVersionScriptByOperation(scriptVersionVo);
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
        Long id = autoexecCombopVo.getId();
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        Map<String, AutoexecCombopGroupVo> groupMap = new HashMap<>();
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo autoexecCombopGroupVo : combopGroupList) {
                if (isCopy) {
                    autoexecCombopGroupVo.setId(null);
                }
                autoexecCombopGroupVo.setCombopId(id);
                autoexecCombopMapper.insertAutoexecCombopGroup(autoexecCombopGroupVo);
                groupMap.put(autoexecCombopGroupVo.getUuid(), autoexecCombopGroupVo);
            }
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo != null) {
                    if (isCopy) {
                        autoexecCombopPhaseVo.setId(null);
                    }
                    autoexecCombopPhaseVo.setCombopId(id);
                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        Long combopPhaseId = autoexecCombopPhaseVo.getId();
                        int jSort = 0;
                        for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                            if (autoexecCombopPhaseOperationVo != null) {
                                autoexecCombopPhaseOperationVo.setOperationId(null);
                                autoexecCombopPhaseOperationVo.setSort(jSort++);
                                autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
                                autoexecCombopMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
                            }
                        }
                    }
                    AutoexecCombopGroupVo autoexecCombopGroupVo = groupMap.get(autoexecCombopPhaseVo.getGroupUuid());
                    if (autoexecCombopGroupVo != null) {
                        autoexecCombopPhaseVo.setGroupId(autoexecCombopGroupVo.getId());
                    }
                    autoexecCombopMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
                }
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
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
                if (autoexecCombopPhaseOperationVo != null) {
                    saveDependency(autoexecCombopVo, autoexecCombopPhaseVo, autoexecCombopPhaseOperationVo);
                }
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     * @param combopPhaseVo
     * @param phaseOperationVo
     */
    private void saveDependency(AutoexecCombopVo autoexecCombopVo, AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo) {
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
            DependencyManager.insert(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getOperationId(), dependencyConfig);
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
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getOperationId(), dependencyConfig);
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
                    DependencyManager.insert(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getOperationId(), dependencyConfig);
                }
            }
        }
    }

    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系
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
                if (phaseOperationVo != null) {
                    DependencyManager.delete(AutoexecProfile2CombopPhaseOperationDependencyHandler.class, phaseOperationVo.getOperationId());
                    DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationInputParamDependencyHandler.class, phaseOperationVo.getOperationId());
                    DependencyManager.delete(AutoexecGlobalParam2CombopPhaseOperationArgumentParamDependencyHandler.class, phaseOperationVo.getOperationId());
                }
            }
        }
    }

    @Override
    public String getOperationActiveVersionScriptByOperation(AutoexecScriptVersionVo scriptVersionVo) {
        List<AutoexecScriptLineVo> scriptLineVoList = autoexecScriptMapper.getLineListByVersionId(scriptVersionVo.getId());
        StringBuilder scriptSb = new StringBuilder();
        for (AutoexecScriptLineVo lineVo : scriptLineVoList) {
            if (StringUtils.isNotBlank(lineVo.getContent())) {
                scriptSb.append(lineVo.getContent()).append("\n");
            }
        }
        return scriptSb.toString();
    }

    @Override
    public AutoexecCombopVo getAutoexecCombopById(Long id) {
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            return null;
        }
        autoexecCombopVo.setOwner(GroupSearch.USER.getValuePlugin() + autoexecCombopVo.getOwner());
        List<AutoexecCombopParamVo> runtimeParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
        for (AutoexecCombopParamVo autoexecCombopParamVo : runtimeParamList) {
            autoexecService.mergeConfig(autoexecCombopParamVo);
        }
        autoexecCombopVo.setRuntimeParamList(runtimeParamList);
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        config.setRuntimeParamList(runtimeParamList);
        autoexecService.updateAutoexecCombopConfig(config);
        return autoexecCombopVo;
    }
}
