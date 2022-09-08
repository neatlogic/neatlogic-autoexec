/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.framework.util.UuidUtil;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecProfileService;
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
                throw new AutoexecToolinactivatedException(autoexecToolVo.getName());
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
            throw new AutoexecCombopCannotBeRepeatReleaseExcepiton(autoexecToolAndScriptVo.getName());
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
        List<AutoexecCombopParamVo> autoexecCombopParamVoList = new ArrayList<>();
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
                        autoexecCombopParamVoList.add(new AutoexecCombopParamVo(inputParamVo));
                        paramMappingList.add(new ParamMappingVo(inputParamVo.getKey(), ParamMappingMode.RUNTIME_PARAM.getValue(), inputParamVo.getKey()));
                    }
                }
            } else {
                for (AutoexecParamVo inputParamVo : inputParamList) {
                    autoexecCombopParamVoList.add(new AutoexecCombopParamVo(inputParamVo));
                    paramMappingList.add(new ParamMappingVo(inputParamVo.getKey(), ParamMappingMode.RUNTIME_PARAM.getValue(), inputParamVo.getKey()));
                }
            }
        }
        //argumentMappingList
        List<ParamMappingVo> argumentMappingList = new ArrayList<>();
        operationConfigVo.setArgumentMappingList(argumentMappingList);
        if (argumentParam != null) {
            autoexecToolAndScriptVo.setArgument(argumentParam);
            int sort = autoexecCombopParamVoList.size();
            int argumentCount = argumentParam.getArgumentCount();
            for (int i = 0; i < argumentCount; i++) {
                AutoexecCombopParamVo autoexecCombopParamVo = new AutoexecCombopParamVo();
                autoexecCombopParamVo.setKey("argument_" + i);
                autoexecCombopParamVo.setName(argumentParam.getName() + "_" + i);
                autoexecCombopParamVo.setDefaultValue(argumentParam.getDefaultValue());
                autoexecCombopParamVo.setType(argumentParam.getType());
                autoexecCombopParamVo.setDescription(argumentParam.getDescription());
                autoexecCombopParamVo.setSort(sort++);
                autoexecCombopParamVo.setMode(argumentParam.getMode());
                autoexecCombopParamVo.setIsRequired(argumentParam.getIsRequired());
                autoexecCombopParamVoList.add(autoexecCombopParamVo);
                ParamMappingVo paramMappingVo = new ParamMappingVo();
                paramMappingVo.setName(autoexecCombopParamVo.getName());
                paramMappingVo.setMappingMode(ParamMappingMode.RUNTIME_PARAM.getValue());
                paramMappingVo.setValue(autoexecCombopParamVo.getKey());
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
        if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            throw new AutoexecCombopNameRepeatException(autoexecCombopVo.getName());
        }
        AutoexecCombopConfigVo config = new AutoexecCombopConfigVo();
        List<AutoexecCombopPhaseVo> combopPhaseList = new ArrayList<>();
        combopPhaseList.add(combopPhaseVo);
        config.setCombopPhaseList(combopPhaseList);
        AutoexecCombopExecuteConfigVo executeConfigVo = new AutoexecCombopExecuteConfigVo();
        executeConfigVo.setWhenToSpecify(CombopNodeSpecify.RUNTIME.getValue());
        config.setExecuteConfig(executeConfigVo);

        List<AutoexecCombopGroupVo> autoexecCombopGroupList = new ArrayList<>();
        autoexecCombopGroupList.add(combopGroupVo);
        config.setCombopGroupList(autoexecCombopGroupList);

        autoexecCombopVo.setConfig(config);
        autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
        autoexecCombopService.saveDependency(autoexecCombopVo);

        if (CollectionUtils.isNotEmpty(autoexecCombopParamVoList)) {
            int sort = 0;
            for (AutoexecCombopParamVo autoexecCombopParamVo : autoexecCombopParamVoList) {
                autoexecCombopParamVo.setCombopId(combopId);
                autoexecCombopParamVo.setSort(sort++);
            }
            autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
        }
        autoexecCombopMapper.updateAutoexecCombopIsActiveById(autoexecCombopVo);
        autoexecCombopMapper.insertAutoexecOperationGenerateCombop(combopId, autoexecToolAndScriptVo.getType(), autoexecToolAndScriptVo.getId());
        return combopId;
    }
}
