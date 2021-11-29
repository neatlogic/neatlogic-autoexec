/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.service.AuthenticationInfoService;
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

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    @Override
    public void setOperableButtonList(AutoexecCombopVo autoexecCombopVo) {
        String userUuid = UserContext.get().getUserUuid(true);
        if (Objects.equals(autoexecCombopVo.getOwner(), userUuid)) {
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
        Map<String, AutoexecParamVo> preNodeOutputParamMap = new HashMap<>();
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
            String uuid = autoexecCombopPhaseVo.getUuid();
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                if (autoexecCombopPhaseOperationVo == null) {
                    continue;
                }
                Long operationId = autoexecCombopPhaseOperationVo.getOperationId();
                String operationUuid = autoexecCombopPhaseOperationVo.getUuid();
                String operationName = autoexecCombopPhaseOperationVo.getName();
                List<? extends AutoexecParamVo> autoexecParamVoList = null;
                Map<String, AutoexecParamVo> inputParamMap = new HashMap<>();
                Map<String, String> inputParamNameMap = new HashMap<>();
                AutoexecParamVo argumentParam = null;
                if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                    if (autoexecScriptMapper.checkScriptIsExistsById(operationId) == 0) {
                        throw new AutoexecScriptNotFoundException(operationId);
                    }
                    autoexecParamVoList = autoexecScriptMapper.getParamListByScriptId(operationId);
                    //TODO 自定义脚本需后续支持自由参数
                } else {
                    AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(operationId);
                    if (autoexecToolVo == null) {
                        throw new AutoexecToolNotFoundException(operationId);
                    }
                    JSONObject toolConfig = autoexecToolVo.getConfig();
                    if (MapUtils.isNotEmpty(toolConfig)) {
                        JSONArray paramArray = toolConfig.getJSONArray("paramList");
                        if (CollectionUtils.isNotEmpty(paramArray)) {
                            autoexecParamVoList = paramArray.toJavaList(AutoexecParamVo.class);
                        }
                        JSONObject argumentJson = toolConfig.getJSONObject("argument");
                        if (MapUtils.isNotEmpty(argumentJson)) {
                            argumentParam = new AutoexecParamVo(argumentJson);
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
                        }
                    }
                }

                AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                if (operationConfig != null) {
                    //验证输入参数
                    List<ParamMappingVo> paramMappingList = operationConfig.getParamMappingList();
                    validateParam(paramMappingList, inputParamMap, null, runtimeParamMap, preNodeOutputParamMap);
                    //验证自由参数
                    List<ParamMappingVo> argumentMappingList = operationConfig.getArgumentMappingList();
                    validateParam(argumentMappingList, inputParamMap, argumentParam, runtimeParamMap, preNodeOutputParamMap);
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
                    throw new AutoexecParamMappingNotMappedException(String.join("、", inputParamSet));
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = config.getExecuteConfig();
        if (executeConfigVo != null) {
            if (Objects.equals(executeConfigVo.getWhenToSpecify(), CombopNodeSpecify.NOW.getValue())) {
                String executeUser = executeConfigVo.getExecuteUser();
                if (StringUtils.isBlank(executeUser)) {
                    throw new AutoexecCombopExecuteUserCannotBeEmptyException();
                }
                AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                if (executeNodeConfigVo == null) {
                    throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
                }
                List<AutoexecNodeVo> selectNodeList = executeNodeConfigVo.getSelectNodeList();
                List<AutoexecNodeVo> inputNodeList = executeNodeConfigVo.getInputNodeList();
                JSONObject filter = executeNodeConfigVo.getFilter();
                if (CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && MapUtils.isEmpty(filter)) {
                    throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
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
     */
    private void validateParam(List<ParamMappingVo> mappingList, Map<String, AutoexecParamVo> inputParamMap, AutoexecParamVo argumentParam, Map<String, AutoexecCombopParamVo> runtimeParamMap, Map<String, AutoexecParamVo> preNodeOutputParamMap) {
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
                        throw new AutoexecParamNotFoundException(key);
                    }
                } else {
                    inputParamVo = argumentParam;
                    key = "argument";
                }
                String mappingMode = paramMappingVo.getMappingMode();
                if (Objects.equals(mappingMode, ParamMappingMode.IS_EMPTY.getValue())) {
                    if (Objects.equals(inputParamVo.getIsRequired(), 1)) {
                        throw new AutoexecParamMappingIncorrectException(key);
                    }
                    continue;
                }
                Object valueObj = paramMappingVo.getValue();
                if (Objects.equals(mappingMode, ParamMappingMode.CONSTANT.getValue())) {
                    if (valueObj == null) {
                        throw new AutoexecParamMappingIncorrectException(key);
                    } else if (valueObj instanceof String) {
                        if (StringUtils.isBlank((String) valueObj)) {
                            throw new AutoexecParamMappingIncorrectException(key);
                        }
                    } else if (valueObj instanceof JSONArray) {
                        if (CollectionUtils.isEmpty((JSONArray) valueObj)) {
                            throw new AutoexecParamMappingIncorrectException(key);
                        }
                    } else if (valueObj instanceof JSONObject) {
                        if (MapUtils.isEmpty((JSONObject) valueObj)) {
                            throw new AutoexecParamMappingIncorrectException(key);
                        }
                    }
                    continue;
                }
                String value = (String) valueObj;
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
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingIncorrectException(key);
                    }
                    if (!Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingIncorrectException(key);
                    }
                } else if (Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM_KEY.getValue())) {
                    AutoexecParamVo preNodeOutputParamVo = preNodeOutputParamMap.get(value);
                    if (preNodeOutputParamVo == null) {
                        throw new AutoexecParamMappingIncorrectException(key);
                    }
                    if (!Objects.equals(preNodeOutputParamVo.getType(), inputParamVo.getType())) {
                        throw new AutoexecParamMappingIncorrectException(key);
                    }
                } else {
                    throw new AutoexecParamMappingIncorrectException(key);
                }
            }
        }
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
}
