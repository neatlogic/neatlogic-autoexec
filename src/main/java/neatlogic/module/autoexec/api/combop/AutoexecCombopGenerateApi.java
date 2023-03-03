/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecProfileService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 脚本/工具发布生成组合工具接口
 *
 * @author linbq
 * @since 2021/4/21 15:20
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_ADD.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecCombopGenerateApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    private AutoexecToolMapper autoexecToolMapper;
    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;
    @Resource
    private AutoexecProfileService autoexecProfileService;

    @Override
    public String getToken() {
        return "autoexec/combop/generate";
    }

    @Override
    public String getName() {
        return "脚本/工具发布生成组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "脚本/工具主键id"),
            @Param(name = "operationType", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "脚本/工具"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "脚本/工具发布生成组合工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long operationId = jsonObj.getLong("operationId");
        String operationType = jsonObj.getString("operationType");
        AutoexecParamVo argumentParam = null;
        if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
            if (autoexecScriptVo == null) {
                throw new AutoexecScriptNotFoundException(operationId);
            }
            AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(autoexecScriptVo.getId());
            if (autoexecScriptVersionVo == null) {
                throw new AutoexecScriptVersionHasNoActivedException(autoexecScriptVo.getName());
            }
            List<AutoexecScriptVersionParamVo> autoexecScriptVersionParamVoList = autoexecScriptMapper.getParamListByScriptId(operationId);
            argumentParam = autoexecScriptMapper.getArgumentByVersionId(autoexecScriptVersionVo.getId());
            return generate(jsonObj, new AutoexecOperationVo(autoexecScriptVo), autoexecScriptVersionParamVoList, argumentParam);
        } else {
            AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(operationId);
            if (autoexecToolVo == null) {
                throw new AutoexecToolNotFoundException(operationId);
            }
            if (Objects.equals(autoexecToolVo.getIsActive(), 0)) {
                throw new AutoexecToolNotActiveException(autoexecToolVo.getName());
            }
            List<AutoexecParamVo> autoexecParamVoList = new ArrayList<>();
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
            return generate(jsonObj, new AutoexecOperationVo(autoexecToolVo), autoexecParamVoList, argumentParam);
        }
    }

    public IValid name() {
        return jsonObj -> {
            AutoexecCombopVo autoexecCombopVo = new AutoexecCombopVo();
            autoexecCombopVo.setName(jsonObj.getString("name"));
            if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopNameRepeatException(autoexecCombopVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

    private Long generate(JSONObject jsonObj, AutoexecOperationVo autoexecToolAndScriptVo, List<? extends AutoexecParamVo> autoexecParamVoList, AutoexecParamVo argumentParam) {
        if (autoexecCombopMapper.checkItHasBeenGeneratedToCombopByOperationId(autoexecToolAndScriptVo.getId()) != null) {
            throw new AutoexecCombopCannotBeRepeatReleaseException(autoexecToolAndScriptVo.getName());
        }
        /** 新建一个操作 **/
        AutoexecCombopPhaseOperationVo phaseOperationVo = new AutoexecCombopPhaseOperationVo();
        phaseOperationVo.setUuid(UuidUtil.randomUuid());
        phaseOperationVo.setOperationId(autoexecToolAndScriptVo.getId());
        phaseOperationVo.setOperationName(autoexecToolAndScriptVo.getName());
        phaseOperationVo.setOperationType(autoexecToolAndScriptVo.getType());
        phaseOperationVo.setFailPolicy(FailPolicy.STOP.getValue());
        phaseOperationVo.setSort(0);
        AutoexecRiskVo riskVo = autoexecRiskMapper.getAutoexecRiskById(autoexecToolAndScriptVo.getRiskId());
        autoexecToolAndScriptVo.setRiskVo(riskVo);
        AutoexecCombopPhaseOperationConfigVo operationConfigVo = new AutoexecCombopPhaseOperationConfigVo();
        //paramMappingList
        List<ParamMappingVo> paramMappingList = new ArrayList<>();
        operationConfigVo.setParamMappingList(paramMappingList);
        List<AutoexecParamVo> inputParamList = new ArrayList<>();
        List<AutoexecParamVo> outputParamList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(autoexecParamVoList)) {
            for (AutoexecParamVo paramVo : autoexecParamVoList) {
                String mode = paramVo.getMode();
                if (Objects.equals(mode, ParamMode.INPUT.getValue())) {
                    inputParamList.add(paramVo);
                } else if (Objects.equals(mode, ParamMode.OUTPUT.getValue())) {
                    outputParamList.add(paramVo);
                }
            }
        }
        autoexecToolAndScriptVo.setInputParamList(inputParamList);
        autoexecToolAndScriptVo.setOutputParamList(outputParamList);
        List<AutoexecParamVo> runtimeParamList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(inputParamList)) {
            Long defaultProfileId = autoexecToolAndScriptVo.getDefaultProfileId();
            if (defaultProfileId != null) {
                operationConfigVo.setProfileId(defaultProfileId);
                operationConfigVo.setProfileName(autoexecToolAndScriptVo.getDefaultProfileName());
                Map<String, AutoexecProfileParamVo> profileParamMap = new HashMap<>();
                List<AutoexecProfileParamVo> profileParamList = autoexecProfileService.getProfileParamListById(defaultProfileId);
                if (CollectionUtils.isNotEmpty(profileParamList)) {
                    profileParamMap = profileParamList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
                }
                for (AutoexecParamVo inputParamVo : inputParamList) {
                    AutoexecProfileParamVo autoexecProfileParamVo = profileParamMap.get(inputParamVo.getKey());
                    if (autoexecProfileParamVo != null) {
                        paramMappingList.add(new ParamMappingVo(inputParamVo.getKey(), ParamMappingMode.PROFILE.getValue(), autoexecProfileParamVo.getDefaultValueStr()));
                    } else {
                        runtimeParamList.add(inputParamVo);
                        paramMappingList.add(new ParamMappingVo(inputParamVo.getKey(), ParamMappingMode.RUNTIME_PARAM.getValue(), inputParamVo.getKey()));
                    }
                }
            } else {
                for (AutoexecParamVo inputParamVo : inputParamList) {
                    runtimeParamList.add(inputParamVo);
                    paramMappingList.add(new ParamMappingVo(inputParamVo.getKey(), ParamMappingMode.RUNTIME_PARAM.getValue(), inputParamVo.getKey()));
                }
            }
        }
        //argumentMappingList
        List<ParamMappingVo> argumentMappingList = new ArrayList<>();
        operationConfigVo.setArgumentMappingList(argumentMappingList);
        if (argumentParam != null) {
            autoexecToolAndScriptVo.setArgument(argumentParam);
            int sort = runtimeParamList.size();
            int argumentCount = argumentParam.getArgumentCount();
            for (int i = 0; i < argumentCount; i++) {
                AutoexecParamVo autoexecParamVo = new AutoexecParamVo();
                autoexecParamVo.setKey("argument_" + i);
                autoexecParamVo.setName(argumentParam.getName() + "_" + i);
                autoexecParamVo.setDefaultValue(argumentParam.getDefaultValue());
                autoexecParamVo.setType(argumentParam.getType());
                autoexecParamVo.setDescription(argumentParam.getDescription());
                autoexecParamVo.setSort(sort++);
                autoexecParamVo.setMode(argumentParam.getMode());
                autoexecParamVo.setIsRequired(argumentParam.getIsRequired());
                runtimeParamList.add(autoexecParamVo);
                ParamMappingVo paramMappingVo = new ParamMappingVo();
                paramMappingVo.setName(autoexecParamVo.getName());
                paramMappingVo.setMappingMode(ParamMappingMode.RUNTIME_PARAM.getValue());
                paramMappingVo.setValue(autoexecParamVo.getKey());
                argumentMappingList.add(paramMappingVo);
            }
        }
        phaseOperationVo.setConfig(operationConfigVo);
        /** 新建一个组 **/
        AutoexecCombopGroupVo combopGroupVo = new AutoexecCombopGroupVo();
        combopGroupVo.setConfig("{}");
        combopGroupVo.setSort(0);
        combopGroupVo.setUuid(UuidUtil.randomUuid());
        combopGroupVo.setPolicy("oneShot");

        /** 新建一个阶段 **/
        AutoexecCombopPhaseVo combopPhaseVo = new AutoexecCombopPhaseVo();
        combopPhaseVo.setUuid(UuidUtil.randomUuid());
        combopPhaseVo.setName("phase-run");
        combopPhaseVo.setExecMode(autoexecToolAndScriptVo.getExecMode());
        combopPhaseVo.setSort(0);
        AutoexecCombopPhaseConfigVo combopPhaseConfig = new AutoexecCombopPhaseConfigVo();
        List<AutoexecCombopPhaseOperationVo> phaseOperationList = new ArrayList<>();
        phaseOperationList.add(phaseOperationVo);
        combopPhaseConfig.setPhaseOperationList(phaseOperationList);
        combopPhaseVo.setConfig(combopPhaseConfig);

        combopPhaseVo.setGroupUuid(combopGroupVo.getUuid());
        combopPhaseVo.setGroupSort(combopGroupVo.getSort());
        combopPhaseVo.setGroupId(combopGroupVo.getId());
        /** 新建一个组合工具 **/
        AutoexecCombopVo autoexecCombopVo = new AutoexecCombopVo(autoexecToolAndScriptVo);
        autoexecCombopVo.setOwner(UserContext.get().getUserUuid(true));
        Long combopId = autoexecCombopVo.getId();
        String name = jsonObj.getString("name");
        Long typeId = jsonObj.getLong("typeId");
        String description = jsonObj.getString("description");
        autoexecCombopVo.setName(name);
        autoexecCombopVo.setTypeId(typeId);
        autoexecCombopVo.setDescription(description);
        autoexecCombopVo.setIsActive(1);
        if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            throw new AutoexecCombopNameRepeatException(autoexecCombopVo.getName());
        }
        AutoexecCombopConfigVo config = new AutoexecCombopConfigVo();
        autoexecCombopVo.setConfig(config);

        AutoexecCombopVersionVo autoexecCombopVersionVo = new AutoexecCombopVersionVo();
        autoexecCombopVersionVo.setCombopId(autoexecCombopVo.getId());
        autoexecCombopVersionVo.setName(name);
        autoexecCombopVersionVo.setVersion(1);
        autoexecCombopVersionVo.setStatus(ScriptVersionStatus.PASSED.getValue());
        autoexecCombopVersionVo.setIsActive(1);
        autoexecCombopVersionVo.setLcu(UserContext.get().getUserUuid(true));
        autoexecCombopVersionVo.setReviewer(UserContext.get().getUserUuid(true));

        AutoexecCombopVersionConfigVo versionConfig = new AutoexecCombopVersionConfigVo();
        List<AutoexecCombopPhaseVo> combopPhaseList = new ArrayList<>();
        combopPhaseList.add(combopPhaseVo);
        versionConfig.setCombopPhaseList(combopPhaseList);
        AutoexecCombopExecuteConfigVo executeConfigVo = new AutoexecCombopExecuteConfigVo();
        executeConfigVo.setWhenToSpecify(CombopNodeSpecify.RUNTIME.getValue());
        versionConfig.setExecuteConfig(executeConfigVo);

        List<AutoexecCombopGroupVo> autoexecCombopGroupList = new ArrayList<>();
        autoexecCombopGroupList.add(combopGroupVo);
        versionConfig.setCombopGroupList(autoexecCombopGroupList);

        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            int sort = 0;
            for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
                autoexecParamVo.setEditable(1);
                autoexecParamVo.setSort(sort++);
            }
            versionConfig.setRuntimeParamList(runtimeParamList);
        }
        autoexecCombopVersionVo.setConfig(versionConfig);
        autoexecCombopVersionMapper.insertAutoexecCombopVersion(autoexecCombopVersionVo);
        autoexecCombopService.saveDependency(autoexecCombopVersionVo);
        autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
        autoexecCombopService.saveDependency(autoexecCombopVo);
        autoexecCombopMapper.insertAutoexecOperationGenerateCombop(combopId, autoexecToolAndScriptVo.getType(), autoexecToolAndScriptVo.getId());
        return combopId;
    }
}
