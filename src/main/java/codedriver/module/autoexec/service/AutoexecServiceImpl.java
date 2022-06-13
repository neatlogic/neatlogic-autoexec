/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecParamMappingNotFoundException;
import codedriver.framework.autoexec.script.paramtype.IScriptParamType;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import codedriver.framework.exception.type.*;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Service
public class AutoexecServiceImpl implements AutoexecService, IAutoexecServiceCrossoverService {

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    AutoexecToolMapper autoexecToolMapper;
    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;
    @Resource
    AutoexecScenarioMapper autoexecScenarioMapper;
    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    static Pattern paramKeyPattern = Pattern.compile("^[A-Za-z_\\d]+$");

    static Pattern paramNamePattern = Pattern.compile("^[A-Za-z_\\d\\u4e00-\\u9fa5]+$");

    /**
     * 校验参数列表
     *
     * @param paramList
     */
    @Override
    public void validateParamList(List<? extends AutoexecParamVo> paramList) {
        if (CollectionUtils.isNotEmpty(paramList)) {
            List<? extends AutoexecParamVo> inputParamList = paramList.stream().filter(o -> Objects.equals(ParamMode.INPUT.getValue(), o.getMode())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(inputParamList)) {
                doValidateParamList(inputParamList);
            }
            List<? extends AutoexecParamVo> outParamList = paramList.stream().filter(o -> Objects.equals(ParamMode.OUTPUT.getValue(), o.getMode())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(outParamList)) {
                doValidateParamList(outParamList);
            }
        }
    }

    private void doValidateParamList(List<? extends AutoexecParamVo> paramList) {
        Set<String> keySet = new HashSet<>(paramList.size());
        for (int i = 0; i < paramList.size(); i++) {
            AutoexecParamVo param = paramList.get(i);
            if (param != null) {
                String mode = param.getMode();
                String key = param.getKey();
                String name = param.getName();
                String type = param.getType();
                Integer isRequired = param.getIsRequired();
                String mappingMode = param.getMappingMode();
                int index = i + 1;
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException(index, "英文名");
                }
                if (keySet.contains(key)) {
                    throw new ParamRepeatsException(key);
                } else {
                    keySet.add(key);
                }
                if (!paramKeyPattern.matcher(key).matches()) {
                    throw new ParamIrregularException(key);
                }
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException(index, key, "中文名");
                }
                if (!paramNamePattern.matcher(name).matches()) {
                    throw new ParamIrregularException(index, key, name);
                }
                if (param instanceof AutoexecScriptVersionParamVo && StringUtils.isBlank(mode)) {
                    throw new ParamNotExistsException(index, key, "参数模式");
                }
                if (StringUtils.isNotBlank(mode) && ParamMode.getParamMode(mode) == null) {
                    throw new ParamIrregularException(index, key, mode);
                }
                if (StringUtils.isBlank(type)) {
                    throw new ParamNotExistsException(index, key, "控件类型");
                }
                if (ParamMode.INPUT.getValue().equals(param.getMode())) {
                    ParamType paramType = ParamType.getParamType(type);
                    if (paramType == null) {
                        throw new ParamIrregularException(index, key, type);
                    }
                    if (isRequired == null) {
                        throw new ParamNotExistsException(index, key, "是否必填");
                    }
                    if (mappingMode != null && AutoexecProfileParamInvokeType.getParamType(mappingMode) == null) {
                        throw new AutoexecParamMappingNotFoundException(key, mappingMode);
                    }
                } else {
                    OutputParamType paramType = OutputParamType.getParamType(type);
                    if (paramType == null) {
                        throw new ParamIrregularException(index, key, type);
                    }
                }
            }
        }
    }

    @Override
    public void validateArgument(AutoexecParamVo argument) {
        if (argument != null) {
            String name = argument.getName();
            Integer argumentCount = argument.getArgumentCount();
            String description = argument.getDescription();
            String defaultValueStr = argument.getDefaultValueStr();
            if (StringUtils.isBlank(name)) {
                throw new ParamNotExistsException("argument.name");
            }
            if (name.length() > 50) {
                throw new ParamValueTooLongException("argument.name", name.length(), 50);
            }
            if (argumentCount != null && argumentCount < 0) {
                throw new ParamInvalidException("argument.argumentCount", argumentCount.toString());
            }
            if (defaultValueStr != null && defaultValueStr.length() > 200) {
                throw new ParamValueTooLongException("argument.defaultValue", defaultValueStr.length(), 200);
            }
            if (description != null && description.length() > 500) {
                throw new ParamValueTooLongException("argument.description", description.length(), 500);
            }
        }
    }

    @Override
    public void mergeConfig(AutoexecParamVo autoexecParamVo) {
        IScriptParamType paramType = ScriptParamTypeFactory.getHandler(autoexecParamVo.getType());
        if (paramType != null) {
            JSONObject paramTypeConfig = new JSONObject(paramType.needDataSource());
            if (Objects.equals(autoexecParamVo.getIsRequired(), 0)) {
                paramTypeConfig.put("isRequired", false);
            } else {
                paramTypeConfig.put("isRequired", true);
            }
            paramTypeConfig.put("type", paramType.getType());
            JSONObject config = autoexecParamVo.getConfig();
            if (config == null) {
                autoexecParamVo.setConfig(paramTypeConfig.toJSONString());
            } else {
                if (Objects.equals(config.getString("dataSource"), ParamDataSource.STATIC.getValue())) {
                    paramTypeConfig.remove("url");
                    paramTypeConfig.remove("dynamicUrl");
                    paramTypeConfig.remove("rootName");
                }
                config.putAll(paramTypeConfig);
            }
        }
    }


    @Override
    public void updateAutoexecCombopConfig(AutoexecCombopConfigVo config) {
        List<AutoexecCombopScenarioVo> combopScenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(combopScenarioList)) {
            for (AutoexecCombopScenarioVo combopScenarioVo : combopScenarioList) {
                Long scenarioId = combopScenarioVo.getScenarioId();
                if (scenarioId != null) {
                    AutoexecScenarioVo autoexecScenarioVo = autoexecScenarioMapper.getScenarioById(scenarioId);
                    if (autoexecScenarioVo != null) {
                        combopScenarioVo.setScenarioName(autoexecScenarioVo.getName());
                    }
                }
            }
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                if (phaseConfigVo != null) {
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfigVo.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                            AutoexecParamVo argumentParam = null;
                            AutoexecOperationVo autoexecToolAndScriptVo = null;
                            List<? extends AutoexecParamVo> autoexecParamVoList = new ArrayList<>();
                            if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                                AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(phaseOperationVo.getOperationId());
                                if (autoexecScriptVo != null) {
                                    autoexecToolAndScriptVo = new AutoexecOperationVo(autoexecScriptVo);
                                    autoexecParamVoList = autoexecScriptMapper.getParamListByScriptId(phaseOperationVo.getOperationId());
                                    AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(phaseOperationVo.getOperationId());
                                    if (autoexecScriptVersionVo != null) {
                                        argumentParam = autoexecScriptMapper.getArgumentByVersionId(autoexecScriptVersionVo.getId());
                                    }
                                }
                            } else if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                                AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(phaseOperationVo.getOperationId());
                                if (autoexecToolVo != null) {
                                    autoexecToolAndScriptVo = new AutoexecOperationVo(autoexecToolVo);
                                    JSONObject toolConfig = autoexecToolVo.getConfig();
                                    if (MapUtils.isNotEmpty(toolConfig)) {
                                        JSONArray paramArray = toolConfig.getJSONArray("paramList");
                                        if (CollectionUtils.isNotEmpty(paramArray)) {
                                            autoexecParamVoList = paramArray.toJavaList(AutoexecParamVo.class);
                                        }
                                        JSONObject argumentJson = toolConfig.getJSONObject("argument");
                                        if (MapUtils.isNotEmpty(argumentJson)) {
                                            argumentParam = JSONObject.toJavaObject(argumentJson, AutoexecParamVo.class);
                                        }
                                    }
                                }
                            }
                            if (autoexecToolAndScriptVo != null) {
                                phaseOperationVo.setId(autoexecToolAndScriptVo.getId());
                                phaseOperationVo.setUk(autoexecToolAndScriptVo.getUk());
                                phaseOperationVo.setName(autoexecToolAndScriptVo.getName());
                                phaseOperationVo.setType(CombopOperationType.SCRIPT.getValue());
                                phaseOperationVo.setExecMode(autoexecToolAndScriptVo.getExecMode());
                                phaseOperationVo.setTypeId(autoexecToolAndScriptVo.getTypeId());
                                phaseOperationVo.setTypeName(autoexecToolAndScriptVo.getTypeName());
                                phaseOperationVo.setRiskId(autoexecToolAndScriptVo.getRiskId());
                                AutoexecRiskVo riskVo = autoexecRiskMapper.getAutoexecRiskById(autoexecToolAndScriptVo.getRiskId());
                                phaseOperationVo.setRiskVo(riskVo);

                                List<AutoexecParamVo> inputParamList = new ArrayList<>();
                                List<AutoexecParamVo> outputParamList = new ArrayList<>();
                                if (CollectionUtils.isNotEmpty(autoexecParamVoList)) {
                                    for (AutoexecParamVo paramVo : autoexecParamVoList) {
                                        mergeConfig(paramVo);
                                        String mode = paramVo.getMode();
                                        if (Objects.equals(mode, ParamMode.INPUT.getValue())) {
                                            inputParamList.add(paramVo);
                                        } else if (Objects.equals(mode, ParamMode.OUTPUT.getValue())) {
                                            outputParamList.add(paramVo);
                                        }
                                    }
                                }
                                phaseOperationVo.setInputParamList(inputParamList);
                                phaseOperationVo.setOutputParamList(outputParamList);
                                phaseOperationVo.setArgument(argumentParam);
                            }
                            AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
                            if (operationConfigVo != null) {
                                Long profileId = operationConfigVo.getProfileId();
                                if (profileId != null) {
                                    AutoexecProfileVo autoexecProfileVo = autoexecProfileMapper.getProfileVoById(profileId);
                                    if (autoexecProfileVo != null) {
                                        operationConfigVo.setProfileName(autoexecProfileVo.getName());
                                    }
                                }
                            }
                        }
                    }
                }
                combopPhaseVo.setExecModeName(ExecMode.getText(combopPhaseVo.getExecMode()));
            }
        }
    }

    @Override
    public List<AutoexecParamVo> getAutoexecOperationParamVoList(List<AutoexecOperationVo> paramAutoexecOperationVoList) {
        List<Long> toolIdList = paramAutoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = paramAutoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //获取新的参数列表
        List<AutoexecParamVo> newOperationParamVoList = new ArrayList<>();
        List<AutoexecOperationVo> autoexecOperationVoList = getAutoexecOperationByScriptIdAndToolIdList(scriptIdList, toolIdList);

        if (CollectionUtils.isNotEmpty(autoexecOperationVoList)) {
            for (AutoexecOperationVo operationVo : autoexecOperationVoList) {
                List<AutoexecParamVo> inputParamList = operationVo.getInputParamList();
                if (CollectionUtils.isNotEmpty(inputParamList)) {
                    for (AutoexecParamVo paramVo : inputParamList) {
                        paramVo.setOperationId(operationVo.getId());
                        paramVo.setOperationType(operationVo.getType());
                    }
                }
                if (CollectionUtils.isNotEmpty(inputParamList)) {
                    newOperationParamVoList.addAll(inputParamList);
                }
            }
        }
        return newOperationParamVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecParamVo::getKey))), ArrayList::new));
    }

    /**
     * 根据scriptIdList和toolIdList获取对应的operationVoList
     *
     * @param scriptIdList
     * @param toolIdList
     * @return
     */
    @Override
    public List<AutoexecOperationVo> getAutoexecOperationByScriptIdAndToolIdList(List<Long> scriptIdList, List<Long> toolIdList) {
        if (CollectionUtils.isEmpty(scriptIdList) && CollectionUtils.isEmpty(toolIdList)) {
            return null;
        }
        List<AutoexecOperationVo> returnList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            returnList.addAll(autoexecScriptMapper.getAutoexecOperationListByIdList(scriptIdList));
            //补输入和输出参数
            Map<Long, AutoexecOperationVo> autoexecOperationInputParamMap = autoexecScriptMapper.getAutoexecOperationInputParamListByIdList(scriptIdList).stream().collect(Collectors.toMap(AutoexecOperationVo::getId, e -> e));
            Map<Long, AutoexecOperationVo> autoexecOperationOutputParamMap = autoexecScriptMapper.getAutoexecOperationOutputParamListByIdList(scriptIdList).stream().collect(Collectors.toMap(AutoexecOperationVo::getId, e -> e));
            for (AutoexecOperationVo autoexecOperationVo : returnList) {
                autoexecOperationVo.setInputParamList(autoexecOperationInputParamMap.get(autoexecOperationVo.getId()).getInputParamList());
                autoexecOperationVo.setOutputParamList(autoexecOperationOutputParamMap.get(autoexecOperationVo.getId()).getOutputParamList());
            }
        }

        if (CollectionUtils.isNotEmpty(toolIdList)) {
            returnList.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
        }
        return returnList;
    }

    @Override
    public Long saveProfileOperation(String profileName, Long operatioinId, String operationType) {
        if (StringUtils.isNotBlank(profileName) && operatioinId != null) {
            AutoexecProfileVo profile = autoexecProfileMapper.getProfileVoByName(profileName);
            if (profile == null) {
                profile = new AutoexecProfileVo(profileName, -1L);
                autoexecProfileMapper.insertProfile(profile);
            }
            autoexecProfileMapper.insertAutoexecProfileOperation(profile.getId(), Collections.singletonList(operatioinId), operationType, new Date());
            return profile.getId();
        }
        return null;
    }
}
