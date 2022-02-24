/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopNodeSpecify;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author linbq
 * @since 2021/9/2 18:34
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopProcessConfigInitApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/processconfig/init";
    }

    @Override
    public String getName() {
        return "组合工具流程自动化节点配置初始化";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "runtimeParamList", type = ApiParamType.JSONARRAY, desc = "运行参数列表"),
            @Param(name = "executeParamList", type = ApiParamType.JSONARRAY, desc = "执行参数列表"),
            @Param(name = "exportParamList", type = ApiParamType.JSONARRAY, desc = "输出参数列表")
    })
    @Description(desc = "查询组合工具授权信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long combopId = paramObj.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        JSONObject resultObj = new JSONObject();
        Map<String, AutoexecCombopParamVo> autoexecCombopParamMap = new HashMap<>();
        List<AutoexecCombopParamVo> autoexecCombopParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
        if (CollectionUtils.isNotEmpty(autoexecCombopParamList)) {
            JSONArray runtimeParamList = new JSONArray();
            for (AutoexecCombopParamVo autoexecCombopParamVo : autoexecCombopParamList) {
                JSONObject runtimeParamObj = new JSONObject();
                runtimeParamObj.put("key", autoexecCombopParamVo.getKey());
                runtimeParamObj.put("name", autoexecCombopParamVo.getName());
                Object defaultValue = autoexecCombopParamVo.getDefaultValue();
                if (defaultValue != null) {
                    runtimeParamObj.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                    runtimeParamObj.put("value", defaultValue);
                } else {
                    runtimeParamObj.put("mappingMode", "");
                    runtimeParamObj.put("value", "");
                }
                runtimeParamObj.put("isRequired", autoexecCombopParamVo.getIsRequired());
                runtimeParamObj.put("type", autoexecCombopParamVo.getType());
                runtimeParamObj.put("config", autoexecCombopParamVo.getConfig());
                runtimeParamList.add(runtimeParamObj);
                autoexecCombopParamMap.put(autoexecCombopParamVo.getKey(), autoexecCombopParamVo);
            }
            resultObj.put("runtimeParamList", runtimeParamList);
        }
        AutoexecCombopConfigVo autoexecCombopConfigVo = autoexecCombopVo.getConfig();
        if (autoexecCombopConfigVo == null) {
            return resultObj;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = autoexecCombopConfigVo.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return resultObj;
        }
        boolean allRunner = true;
        JSONArray allExportParamList = new JSONArray();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            String execMode = autoexecCombopPhaseVo.getExecMode();
            if (!ExecMode.RUNNER.getValue().equals(execMode)) {
                allRunner = false;
            }
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo != null) {
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = autoexecCombopPhaseConfigVo.getPhaseOperationList();
                if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        String name = autoexecCombopPhaseOperationVo.getName();
                        if (StringUtils.isNotBlank(name)) {
                            if (name.contains("setenvglobal") || name.contains("setenv")) {
                                AutoexecCombopPhaseOperationConfigVo autoexecCombopPhaseOperationConfigVo = autoexecCombopPhaseOperationVo.getConfig();
                                if (autoexecCombopPhaseOperationConfigVo != null) {
                                    List<ParamMappingVo> paramMappingList = autoexecCombopPhaseOperationConfigVo.getParamMappingList();
                                    if (CollectionUtils.isNotEmpty(paramMappingList)) {
                                        JSONObject exportObj = new JSONObject();
                                        for (ParamMappingVo paramMappingVo : paramMappingList) {
                                            String key = paramMappingVo.getKey();
                                            if ("name".equals(key)) {
                                                exportObj.put("text", paramMappingVo.getValue());
                                                exportObj.put("value", paramMappingVo.getValue());
                                            }
                                        }
                                        allExportParamList.add(exportObj);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        List<String> existedExportParamValueList = new ArrayList<>();
        JSONArray exportParamList = new JSONArray();
        for (int i = allExportParamList.size() - 1; i >= 0; i--) {
            JSONObject exportParamObj = allExportParamList.getJSONObject(i);
            String vaule = exportParamObj.getString("value");
            if (existedExportParamValueList.contains(vaule)) {
                continue;
            }
            existedExportParamValueList.add(vaule);
            exportParamList.add(exportParamObj);
        }
        resultObj.put("exportParamList", exportParamList);
        if (allRunner) {
            return resultObj;
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = autoexecCombopConfigVo.getExecuteConfig();
        if (executeConfigVo != null) {
            JSONArray executeParamList = new JSONArray();
            JSONObject executeNode = new JSONObject();
            executeNode.put("key", "executeNodeConfig");
            executeNode.put("name", "执行目标");
            executeNode.put("isRequired", 1);
            String whenToSpecify = executeConfigVo.getWhenToSpecify();
            if (CombopNodeSpecify.RUNTIME.getValue().equals(whenToSpecify)) {
                //运行时再指定执行目标
                executeNode.put("mappingMode", "");
                executeNode.put("value", "");
            } else {
                if (CombopNodeSpecify.RUNTIMEPARAM.getValue().equals(whenToSpecify)) {
                    //运行参数作为执行目标
                    AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = executeConfigVo.getExecuteNodeConfig();
                    if (executeNodeConfigVo != null) {
                        executeNode.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                        executeNode.put("value", executeNodeConfigVo);
                    } else {
                        executeNode.put("mappingMode", "");
                        executeNode.put("value", "");
                    }
                } else {
                    //现在指定执行目标
                    executeNode.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                    executeNode.put("value", executeConfigVo.getExecuteNodeConfig());
                }
            }
            executeParamList.add(executeNode);
            JSONObject protocol = new JSONObject();
            protocol.put("key", "protocolId");
            protocol.put("name", "连接协议");
            protocol.put("isRequired", 1);
            Long protocolId = executeConfigVo.getProtocolId();
            if (protocolId != null) {
                protocol.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                protocol.put("value", protocolId);
            } else {
                protocol.put("mappingMode", "");
                protocol.put("value", "");
            }
            executeParamList.add(protocol);
            JSONObject executeUserObj = new JSONObject();
            executeUserObj.put("key", "executeUser");
            executeUserObj.put("name", "执行用户");
            executeUserObj.put("isRequired", 1);
            String executeUser = executeConfigVo.getExecuteUser();
            if (StringUtils.isNotBlank(executeUser)) {
                executeUserObj.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                executeUserObj.put("value", executeUser);
            } else {
                executeUserObj.put("mappingMode", "");
                executeUserObj.put("value", "");
            }
            executeParamList.add(executeUserObj);
            resultObj.put("executeParamList", executeParamList);
        }
        return resultObj;
    }
}
