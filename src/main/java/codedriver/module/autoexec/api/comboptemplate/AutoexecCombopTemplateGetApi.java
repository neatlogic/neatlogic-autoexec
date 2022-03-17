/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.dao.mapper.*;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateParamVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopTemplateGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/get";
    }

    @Override
    public String getName() {
        return "查询组合工具模板详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(explode = AutoexecCombopTemplateVo.class, desc = "组合工具模板详情")
    })
    @Description(desc = "查询组合工具模板详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopById(id);
        if (autoexecCombopTemplateVo == null) {
            throw new AutoexecCombopTemplateNotFoundException(id);
        }
//        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
//        if (Objects.equals(autoexecCombopVo.getViewable(), 0)) {
//            throw new PermissionDeniedException();
//        }
//        autoexecCombopTemplateVo.setOwner(GroupSearch.USER.getValuePlugin() + autoexecCombopTemplateVo.getOwner());
        List<AutoexecCombopTemplateParamVo> runtimeParamList = autoexecCombopTemplateMapper.getAutoexecCombopParamListByCombopId(id);
        for (AutoexecCombopTemplateParamVo autoexecCombopTemplateParamVo : runtimeParamList) {
            autoexecService.mergeConfig(autoexecCombopTemplateParamVo);
        }
        autoexecCombopTemplateVo.setRuntimeParamList(runtimeParamList);
        AutoexecCombopConfigVo config = autoexecCombopTemplateVo.getConfig();
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
                                    if(MapUtils.isNotEmpty(toolConfig)) {
                                        JSONArray paramArray = toolConfig.getJSONArray("paramList");
                                        if (CollectionUtils.isNotEmpty(paramArray)) {
                                            autoexecParamVoList = paramArray.toJavaList(AutoexecParamVo.class);
                                        }
                                    }
                                }
                            }
                            if(autoexecToolAndScriptVo != null){
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
                                        autoexecService.mergeConfig(paramVo);
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
//                autoexecCombopService.needExecuteConfig(autoexecCombopVo, combopPhaseVo);
            }
        }
        return autoexecCombopTemplateVo;
    }
}
