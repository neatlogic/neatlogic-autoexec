/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dao.mapper.*;
import codedriver.framework.autoexec.dto.*;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.script.paramtype.IScriptParamType;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.ParamRepeatsException;
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
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    AutoexecToolMapper autoexecToolMapper;
    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    static Pattern paramKeyPattern = Pattern.compile("^[A-Za-z_\\d]+$");

    static Pattern paramNamePattern = Pattern.compile("^[A-Za-z_\\d\\u4e00-\\u9fa5]+$");

    /**
     * 校验参数列表
     *
     * @param paramList
     */
    @Override
    public void validateParamList(List<? extends AutoexecParamVo> paramList) {
        Set<String> keySet = new HashSet<>(paramList.size());
        for (int i = 0; i < paramList.size(); i++) {
            AutoexecParamVo param = paramList.get(i);
            if (param != null) {
                String mode = param.getMode();
                String key = param.getKey();
                String name = param.getName();
                String type = param.getType();
                Integer isRequired = param.getIsRequired();
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].key");
                }
                if (keySet.contains(key)) {
                    throw new ParamRepeatsException("paramList.[" + i + "].key");
                } else {
                    keySet.add(key);
                }
                if (!paramKeyPattern.matcher(key).matches()) {
                    throw new ParamIrregularException("paramList.[" + i + "].key");
                }
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].name");
                }
                if (!paramNamePattern.matcher(name).matches()) {
                    throw new ParamIrregularException("paramList.[" + i + "].name");
                }
                if (isRequired == null && !Objects.equals(ParamMode.OUTPUT.getValue(), mode)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].isRequired");
                }
                if (param instanceof AutoexecScriptVersionParamVo && StringUtils.isBlank(mode)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].mode");
                }
                if (StringUtils.isNotBlank(mode) && ParamMode.getParamMode(mode) == null) {
                    throw new ParamIrregularException("paramList.[" + i + "].mode");
                }
                if (StringUtils.isBlank(type)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].type");
                }
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new ParamIrregularException("paramList.[" + i + "].type");
                }
                if (ParamType.TEXT != paramType && ParamMode.OUTPUT.getValue().equals(param.getMode())) {
                    throw new ParamIrregularException("paramList.[" + i + "].type");
                }
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
    public List<AutoexecJobVo> getJobList(AutoexecJobVo jobVo) {
        List<AutoexecJobVo> jobVoList = new ArrayList<>();
        int rowNum = autoexecJobMapper.searchJobCount(jobVo);
        if (rowNum > 0) {
            jobVo.setRowNum(rowNum);
            List<Long> jobIdList = autoexecJobMapper.searchJobId(jobVo);
            if (CollectionUtils.isNotEmpty(jobIdList)) {
                Map<String, ArrayList<Long>> operationIdMap = new HashMap<>();
                jobVoList = autoexecJobMapper.searchJob(jobIdList);
                //补充来源operation信息
                Map<Long, String> operationIdNameMap = new HashMap<>();
                List<AutoexecCombopVo> combopVoList = null;
                List<AutoexecScriptVersionVo> scriptVoList;
                List<AutoexecToolAndScriptVo> toolVoList;
                operationIdMap.put(CombopOperationType.COMBOP.getValue(), new ArrayList<>());
                operationIdMap.put(CombopOperationType.SCRIPT.getValue(), new ArrayList<>());
                operationIdMap.put(CombopOperationType.TOOL.getValue(), new ArrayList<>());
                jobVoList.forEach(o -> {
                    operationIdMap.get(o.getOperationType()).add(o.getOperationId());
                });
                if (CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.COMBOP.getValue()))) {
                    combopVoList = autoexecCombopMapper.getAutoexecCombopByIdList(operationIdMap.get(CombopOperationType.COMBOP.getValue()));
                    combopVoList.forEach(o -> operationIdNameMap.put(o.getId(), o.getName()));
                }
                if (CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.SCRIPT.getValue()))) {
                    scriptVoList = autoexecScriptMapper.getVersionByVersionIdList(operationIdMap.get(CombopOperationType.SCRIPT.getValue()));
                    scriptVoList.forEach(o -> operationIdNameMap.put(o.getId(), o.getTitle()));
                }
                if (CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.TOOL.getValue()))) {
                    toolVoList = autoexecToolMapper.getToolListByIdList(operationIdMap.get(CombopOperationType.TOOL.getValue()));
                    toolVoList.forEach(o -> operationIdNameMap.put(o.getId(), o.getName()));
                }
                Map<Long, AutoexecCombopVo> combopVoMap = null;
                if (CollectionUtils.isNotEmpty(combopVoList)) {
                    combopVoMap = combopVoList.stream().collect(Collectors.toMap(AutoexecCombopVo::getId, o -> o));
                }
                for (AutoexecJobVo vo : jobVoList) {
                    vo.setOperationName(operationIdNameMap.get(vo.getOperationId()));
                    // 如果来源是组合工具，且作业状态是已就绪，那么判断是否有组合工具的执行权限，有执行权限时，出现执行与撤销按钮
                    if (JobStatus.READY.getValue().equals(vo.getStatus()) && MapUtils.isNotEmpty(combopVoMap)) {
                        vo.setIsCanExecute(autoexecCombopService.checkOperableButton(combopVoMap.get(vo.getOperationId()), CombopAuthorityAction.EXECUTE) ? 1 : 0);
                    }
                }
                /*  jobVoList.forEach(j -> {
            //判断是否有编辑权限
            if(Objects.equals(j.getOperationType(), CombopOperationType.COMBOP.getValue())) {
                AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(j.getOperationId());
                if (combopVo == null) {
                    throw new AutoexecCombopNotFoundException(j.getOperationId());
                }
                autoexecCombopService.setOperableButtonList(combopVo);
                if (combopVo.getEditable() == 1) {
                    jobVo.setIsCanEdit(1);
                }
            }
        });*/
            }
        }
        return jobVoList;
    }

    @Override
    public void updateAutoexecCombopConfig(AutoexecCombopConfigVo config) {
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                if (phaseConfigVo != null) {
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfigVo.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                            AutoexecToolAndScriptVo autoexecToolAndScriptVo = null;
                            List<? extends AutoexecParamVo> autoexecParamVoList = new ArrayList<>();
                            if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                                AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(phaseOperationVo.getOperationId());
                                if (autoexecScriptVo != null) {
                                    autoexecToolAndScriptVo = new AutoexecToolAndScriptVo(autoexecScriptVo);
                                    autoexecParamVoList = autoexecScriptMapper.getParamListByScriptId(phaseOperationVo.getOperationId());
                                }
                            } else if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                                AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(phaseOperationVo.getOperationId());
                                if (autoexecToolVo != null) {
                                    autoexecToolAndScriptVo = new AutoexecToolAndScriptVo(autoexecToolVo);
                                    JSONObject toolConfig = autoexecToolVo.getConfig();
                                    if (MapUtils.isNotEmpty(toolConfig)) {
                                        JSONArray paramArray = toolConfig.getJSONArray("paramList");
                                        if (CollectionUtils.isNotEmpty(paramArray)) {
                                            autoexecParamVoList = paramArray.toJavaList(AutoexecParamVo.class);
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
                            }
                        }
                    }
                }
                combopPhaseVo.setExecModeName(ExecMode.getText(combopPhaseVo.getExecMode()));
            }
        }
    }

    @Override
    public List<AutoexecParamVo> getProfileConfig(List<AutoexecOperationVo> paramAutoexecOperationVoList, List<AutoexecParamVo> oldOperationParamList) {

        List<Long> toolIdList = paramAutoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = paramAutoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //获取新的参数列表
        List<AutoexecParamVo> newOperationParamVoList = new ArrayList<>();
        List<AutoexecOperationVo> autoexecOperationVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            autoexecOperationVoList.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            autoexecOperationVoList.addAll(autoexecScriptMapper.getAutoexecOperationListByIdList(scriptIdList));
        }
        for (AutoexecOperationVo operationVo : autoexecOperationVoList) {
            if (CollectionUtils.isNotEmpty(operationVo.getParamVoList())) {
                newOperationParamVoList.addAll(operationVo.getParamVoList());
            }
        }

        //根据name（唯一键）去重
        newOperationParamVoList = newOperationParamVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecParamVo::getName))), ArrayList::new));

        //实时的参数信息
        Map<String, AutoexecParamVo> newOperationParamMap = newOperationParamVoList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));

        //旧的参数信息
        Map<String, AutoexecParamVo> oldOperationParamMap = null;
        if (CollectionUtils.isNotEmpty(oldOperationParamList)) {
            oldOperationParamMap = oldOperationParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));
        }

        //找出需要替换值的参数名称name
        List<String> replaceNameList = new ArrayList<>();
        if (MapUtils.isNotEmpty(newOperationParamMap) && MapUtils.isNotEmpty(oldOperationParamMap)) {
            for (String newParamName : newOperationParamMap.keySet()) {
                if (oldOperationParamMap.containsKey(newParamName) && StringUtils.equals(oldOperationParamMap.get(newParamName).getType(), newOperationParamMap.get(newParamName).getType())) {
                    replaceNameList.add(newParamName);
                }
            }
        }
        //根据参数名称name替换对应的值
        if (CollectionUtils.isNotEmpty(replaceNameList)) {
            for (String name : replaceNameList) {
                newOperationParamMap.get(name).setDefaultValue(oldOperationParamMap.get(name).getDefaultValue());
            }
        }

        List<AutoexecParamVo> returnList = new ArrayList<>();

        for (String name : newOperationParamMap.keySet()) {
            returnList.add(newOperationParamMap.get(name));
        }

        return returnList;
    }

    @Override
    public List<AutoexecParamVo> getProfileConfig(List<AutoexecOperationVo> paramAutoexecOperationVoList) {
        return getProfileConfig(paramAutoexecOperationVoList, null);
    }

}
