/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_EXECUTE;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 查询组合工具详情接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@AuthAction(action = AUTOEXEC_COMBOP_EXECUTE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/get";
    }

    @Override
    public String getName() {
        return "查询组合工具详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(explode = AutoexecCombopVo.class, desc = "组合工具详情")
    })
    @Description(desc = "查询组合工具详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        autoexecCombopVo.setOwner(GroupSearch.USER.getValuePlugin() + autoexecCombopVo.getOwner());
        List<AutoexecCombopParamVo> runtimeParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
        autoexecCombopVo.setRuntimeParamList(runtimeParamList);
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                combopPhaseVo.setExecModeName(ExecMode.getText(combopPhaseVo.getExecMode()));
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                if (phaseConfigVo != null) {
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfigVo.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                            if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                                AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(phaseOperationVo.getOperationId());
                                if (autoexecScriptVo != null) {
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

                                    List<AutoexecScriptVersionParamVo> inputParamList = new ArrayList<>();
                                    List<AutoexecScriptVersionParamVo> outputParamList = new ArrayList<>();
                                    List<AutoexecScriptVersionParamVo> autoexecScriptVersionParamVoList = autoexecScriptMapper.getParamListByScriptId(phaseOperationVo.getOperationId());
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
                                }
                            } else if (Objects.equals(phaseOperationVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                                // TODO linbq 工具的暂时不实现
                            }
                        }
                    }
                }
            }
        }
        return autoexecCombopVo;
    }
}
