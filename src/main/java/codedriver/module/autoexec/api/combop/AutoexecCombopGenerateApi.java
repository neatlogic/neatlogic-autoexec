/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 脚本/工具发布生成组合工具接口
 *
 * @author linbq
 * @since 2021/4/21 15:20
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecCombopGenerateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/generate";
    }

    @Override
    public String getName() {
        return "脚本/工具发布生成组合工具";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 额外配置
     */
    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "脚本/工具主键id"),
            @Param(name = "operationType", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "脚本/工具"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
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
        if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
            if (autoexecScriptVo == null) {
                throw new AutoexecScriptNotFoundException(operationId);
            }
            /** 新建一个操作 **/
            AutoexecCombopPhaseOperationVo phaseOperationVo = new AutoexecCombopPhaseOperationVo();
            phaseOperationVo.setOperationType(CombopOperationType.SCRIPT.getValue());
            phaseOperationVo.setFailPolicy(FailPolicy.STOP.getValue());
            phaseOperationVo.setSort(0);
            phaseOperationVo.setId(autoexecScriptVo.getId());
            phaseOperationVo.setUk(autoexecScriptVo.getUk());
            phaseOperationVo.setName(autoexecScriptVo.getName());
            phaseOperationVo.setType(autoexecScriptVo.getType());
            phaseOperationVo.setExecMode(autoexecScriptVo.getExecMode());
            phaseOperationVo.setTypeId(autoexecScriptVo.getTypeId());
            phaseOperationVo.setTypeName(autoexecScriptVo.getName());
            phaseOperationVo.setRiskId(autoexecScriptVo.getRiskId());
            AutoexecRiskVo riskVo = autoexecRiskMapper.getAutoexecRiskById(autoexecScriptVo.getRiskId());
            phaseOperationVo.setRiskVo(riskVo);
            AutoexecCombopPhaseOperationConfigVo operationConfigVo = new AutoexecCombopPhaseOperationConfigVo();
            List<ParamMappingVo> paramMappingList = new ArrayList<>();
            operationConfigVo.setParamMappingList(paramMappingList);
            List<AutoexecParamVo> inputParamList = new ArrayList<>();
            List<AutoexecParamVo> outputParamList = new ArrayList<>();
            List<AutoexecScriptVersionParamVo> autoexecScriptVersionParamVoList = autoexecScriptMapper.getParamListByScriptId(operationId);
            if (CollectionUtils.isNotEmpty(autoexecScriptVersionParamVoList)) {
                for (AutoexecScriptVersionParamVo paramVo : autoexecScriptVersionParamVoList) {
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
            List<AutoexecCombopParamVo> autoexecCombopParamVoList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(inputParamList)) {
                for (AutoexecParamVo inputParamVo : inputParamList) {
                    autoexecCombopParamVoList.add(new AutoexecCombopParamVo(inputParamVo));
                    paramMappingList.add(new ParamMappingVo(inputParamVo.getKey(), ParamMappingMode.RUNTIME_PARAM.getValue(), inputParamVo.getKey()));
                }
            }
            phaseOperationVo.setConfig(JSONObject.toJSONString(operationConfigVo));

            /** 新建一个阶段 **/
            AutoexecCombopPhaseVo combopPhaseVo = new AutoexecCombopPhaseVo();
//            combopPhaseVo.setUk(autoexecScriptVo.getUk());
            combopPhaseVo.setName(autoexecScriptVo.getName());
            combopPhaseVo.setExecMode(autoexecScriptVo.getExecMode());
            combopPhaseVo.setSort(0);
            AutoexecCombopPhaseConfigVo combopPhaseConfig = new AutoexecCombopPhaseConfigVo();
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = new ArrayList<>();
            phaseOperationList.add(phaseOperationVo);
            combopPhaseConfig.setPhaseOperationList(phaseOperationList);
            combopPhaseVo.setConfig(JSONObject.toJSONString(combopPhaseConfig));

            /** 新建一个组合工具 **/
            AutoexecCombopVo autoexecCombopVo = new AutoexecCombopVo(autoexecScriptVo);
            autoexecCombopVo.setOwner(UserContext.get().getUserUuid(true));
            Long combopId = autoexecCombopVo.getId();
//            if (autoexecCombopMapper.checkAutoexecCombopUkIsRepeat(autoexecCombopVo) != null) {
//                autoexecCombopVo.setUk(autoexecCombopVo.getUk() + "_" + combopId);
//            }
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
            autoexecCombopVo.setConfig(JSONObject.toJSONString(config));
            autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
            combopPhaseVo.setCombopId(combopId);
            autoexecCombopMapper.insertAutoexecCombopPhase(combopPhaseVo);
            phaseOperationVo.setCombopPhaseId(combopPhaseVo.getId());
            autoexecCombopMapper.insertAutoexecCombopPhaseOperation(phaseOperationVo);

            if (CollectionUtils.isNotEmpty(autoexecCombopParamVoList)) {
                int sort = 0;
                for (AutoexecCombopParamVo autoexecCombopParamVo : autoexecCombopParamVoList) {
                    autoexecCombopParamVo.setCombopId(combopId);
                    autoexecCombopParamVo.setSort(sort++);
                }
                autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
            }
            return combopId;
        } else {
            // TODO linbq 工具生成组合工具暂时不做
        }
        return null;
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
}
