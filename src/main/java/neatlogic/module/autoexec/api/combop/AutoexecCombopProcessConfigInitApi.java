/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopNodeSpecify;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;

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
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopId);
        if (activeVersionId == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
        }
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(activeVersionId);
        }
        JSONObject resultObj = new JSONObject();
        AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
        if (versionConfig == null) {
            return resultObj;
        }
        Map<String, AutoexecParamVo> autoexecParamMap = new HashMap<>();
        List<AutoexecParamVo> autoexecParamList = versionConfig.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(autoexecParamList)) {
            JSONArray runtimeParamList = new JSONArray();
            for (AutoexecParamVo autoexecParamVo : autoexecParamList) {
                JSONObject runtimeParamObj = new JSONObject();
                runtimeParamObj.put("key", autoexecParamVo.getKey());
                runtimeParamObj.put("name", autoexecParamVo.getName());
                Object defaultValue = autoexecParamVo.getDefaultValue();
                if (defaultValue != null) {
                    runtimeParamObj.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                    runtimeParamObj.put("value", defaultValue);
                } else {
                    runtimeParamObj.put("mappingMode", "");
                    runtimeParamObj.put("value", "");
                }
                runtimeParamObj.put("isRequired", autoexecParamVo.getIsRequired());
                runtimeParamObj.put("type", autoexecParamVo.getType());
                runtimeParamObj.put("config", autoexecParamVo.getConfig());
                runtimeParamList.add(runtimeParamObj);
                autoexecParamMap.put(autoexecParamVo.getKey(), autoexecParamVo);
            }
            resultObj.put("runtimeParamList", runtimeParamList);
        }

        List<String> phaseNameList = new ArrayList<>();
        List<AutoexecCombopPhaseVo> combopPhaseList = versionConfig.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return resultObj;
        }
        JSONArray allExportParamList = new JSONArray();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            String phaseName = autoexecCombopPhaseVo.getName();
            if (!phaseNameList.contains(phaseName)) {
                phaseNameList.add(phaseName);
            }
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo != null) {
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = autoexecCombopPhaseConfigVo.getPhaseOperationList();
                if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        String name = autoexecCombopPhaseOperationVo.getOperationName();
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
        List<ValueTextVo> phaseList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(phaseNameList)) {
            for (String phaseName : phaseNameList) {
                phaseList.add(new ValueTextVo(phaseName, phaseName));
            }
        }
        resultObj.put("phaseList", phaseList);

        List<AutoexecCombopScenarioVo> combopScenarioList = versionConfig.getScenarioList();
        if (CollectionUtils.isNotEmpty(combopScenarioList)) {
            List<ValueTextVo> scenarioList = new ArrayList<>();
            for (AutoexecCombopScenarioVo combopScenarioVo : combopScenarioList) {
                scenarioList.add(new ValueTextVo(combopScenarioVo.getScenarioId(), combopScenarioVo.getScenarioName()));
            }
            resultObj.put("scenarioList", scenarioList);
            JSONArray scenarioParamList = new JSONArray();
            JSONObject scenarioParam = new JSONObject();
            scenarioParam.put("key", "scenarioId");
            scenarioParam.put("name", "场景");
            scenarioParam.put("isRequired", 1);
            if (versionConfig.getDefaultScenarioId() != null) {
                scenarioParam.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                scenarioParam.put("value", versionConfig.getDefaultScenarioId());
            } else if (combopScenarioList.size() == 1) {
                scenarioParam.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                scenarioParam.put("value", combopScenarioList.get(0).getScenarioId());
            } else {
                scenarioParam.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                scenarioParam.put("value", "");
            }
            scenarioParamList.add(scenarioParam);
            resultObj.put("scenarioParamList", scenarioParamList);
        }

        autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo);
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
        // 判断该组合工具的所有阶段是否都是Runner或SQL执行方式，如果是，直接返回，因为不需要设置执行目标
        if (autoexecCombopVersionVo.getAllPhasesAreRunnerOrSqlExecMode()) {
            resultObj.put("allPhasesAreRunnerOrSqlExecMode", true);
            return resultObj;
        }
        resultObj.put("allPhasesAreRunnerOrSqlExecMode", false);
        // 流程图自动化节点是否需要设置执行用户，只有当有某个非runner类型的阶段，没有设置执行用户时，needExecuteUser=true
        boolean needExecuteUser = autoexecCombopVersionVo.getNeedExecuteUser();
        // 流程图自动化节点是否需要设置连接协议，只有当有某个非runner类型的阶段，没有设置连接协议时，needProtocol=true
        boolean needProtocol = autoexecCombopVersionVo.getNeedProtocol();
        // 流程图自动化节点是否需要设置执行目标，只有当有某个非runner类型的阶段，没有设置执行目标时，needExecuteNode=true
        boolean needExecuteNode = autoexecCombopVersionVo.getNeedExecuteNode();
        // 流程图自动化节点是否需要设置分批数量，只有当有某个非runner类型的阶段，没有设置分批数量时，needRoundCount=true
        boolean needRoundCount = autoexecCombopVersionVo.getNeedRoundCount();
        if (!needExecuteUser && !needProtocol && !needExecuteNode && !needRoundCount) {
            return resultObj;
        }

        JSONArray executeParamList = new JSONArray();
        AutoexecCombopExecuteConfigVo executeConfigVo = versionConfig.getExecuteConfig();
        if (executeConfigVo != null) {
            if (needExecuteNode) {
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
            }
            if (needProtocol) {
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
            }
            if (needExecuteUser) {
                JSONObject executeUserObj = new JSONObject();
                executeUserObj.put("key", "executeUser");
                executeUserObj.put("name", "执行用户");
                executeUserObj.put("isRequired", 1);
                ParamMappingVo executeUser = executeConfigVo.getExecuteUser();
                if (executeUser != null && StringUtils.isNotBlank((String) executeUser.getValue())) {
                    executeUserObj.put("mappingMode", executeUser.getMappingMode());
                    executeUserObj.put("value", executeUser.getValue());
                } else {
                    executeUserObj.put("mappingMode", "");
                    executeUserObj.put("value", "");
                }
                executeParamList.add(executeUserObj);
            }
            if (needRoundCount) {
                JSONObject roundCountObj = new JSONObject();
                roundCountObj.put("key", "roundCount");
                roundCountObj.put("name", "分批数量");
                roundCountObj.put("isRequired", 1);
                Integer roundCount = executeConfigVo.getRoundCount();
                if (roundCount != null) {
                    roundCountObj.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                    roundCountObj.put("value", roundCount);
                } else {
                    roundCountObj.put("mappingMode", "");
                    roundCountObj.put("value", "");
                }
                executeParamList.add(roundCountObj);
            }
        } else {
            if (needExecuteNode) {
                JSONObject executeNode = new JSONObject();
                executeNode.put("key", "executeNodeConfig");
                executeNode.put("name", "执行目标");
                executeNode.put("isRequired", 1);
                //运行时再指定执行目标
                executeNode.put("mappingMode", "");
                executeNode.put("value", "");
                executeParamList.add(executeNode);
            }
            if (needProtocol) {
                JSONObject protocol = new JSONObject();
                protocol.put("key", "protocolId");
                protocol.put("name", "连接协议");
                protocol.put("isRequired", 1);
                protocol.put("mappingMode", "");
                protocol.put("value", "");
                executeParamList.add(protocol);
            }
            if (needExecuteUser) {
                JSONObject executeUserObj = new JSONObject();
                executeUserObj.put("key", "executeUser");
                executeUserObj.put("name", "执行用户");
                executeUserObj.put("isRequired", 1);
                executeUserObj.put("mappingMode", "");
                executeUserObj.put("value", "");
                executeParamList.add(executeUserObj);
            }
            if (needRoundCount) {
                JSONObject roundCountObj = new JSONObject();
                roundCountObj.put("key", "roundCount");
                roundCountObj.put("name", "分批数量");
                roundCountObj.put("isRequired", 1);
                roundCountObj.put("mappingMode", "");
                roundCountObj.put("value", "");
                executeParamList.add(roundCountObj);
            }
        }
        resultObj.put("executeParamList", executeParamList);

        return resultObj;
    }
}
