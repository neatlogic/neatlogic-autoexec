/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopNodeSpecify;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
            @Param(name = "executeParamList", type = ApiParamType.JSONARRAY, desc = "执行参数列表")
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
        List<AutoexecCombopParamVo> autoexecCombopParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
        if (CollectionUtils.isNotEmpty(autoexecCombopParamList)) {
            JSONArray runtimeParamList = new JSONArray();
            for (AutoexecCombopParamVo autoexecCombopParamVo : autoexecCombopParamList) {
                JSONObject runtimeParamObj = new JSONObject();
                runtimeParamObj.put("key", autoexecCombopParamVo.getKey());
                runtimeParamObj.put("name", autoexecCombopParamVo.getName());
                runtimeParamObj.put("mappingMode", "");
                runtimeParamObj.put("value", "");
                runtimeParamObj.put("isRequired", autoexecCombopParamVo.getIsRequired());
                runtimeParamObj.put("type", autoexecCombopParamVo.getType());
                runtimeParamObj.put("config", autoexecCombopParamVo.getConfig());
                runtimeParamList.add(runtimeParamObj);
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
        List<String> existedExportParamValueList = new ArrayList<>();
        JSONArray exportParamList = new JSONArray();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            String execMode = autoexecCombopPhaseVo.getExecMode();
            if (!ExecMode.RUNNER.equals(execMode)) {
                allRunner = false;
            }
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo != null) {
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = autoexecCombopPhaseConfigVo.getPhaseOperationList();
                if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        AutoexecCombopPhaseOperationConfigVo autoexecCombopPhaseOperationConfigVo = autoexecCombopPhaseOperationVo.getConfig();
                        if (autoexecCombopPhaseOperationConfigVo != null) {
                            List<ParamMappingVo> argumentMappingList = autoexecCombopPhaseOperationConfigVo.getArgumentMappingList();
                            if (CollectionUtils.isNotEmpty(argumentMappingList)) {
                                for (ParamMappingVo paramMappingVo : argumentMappingList) {
                                    String value = (String) paramMappingVo.getValue();
                                    if (existedExportParamValueList.contains(value)) {
                                        continue;
                                    }
                                    JSONObject exportObj = new JSONObject();
                                    exportObj.put("value", value);
                                    exportObj.put("text", paramMappingVo.getName());
                                    exportParamList.add(exportObj);
                                }
                            }
                        }
                    }
                }
            }
        }
        resultObj.put("exportParamList", exportParamList);
        if (allRunner) {
            return resultObj;
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = autoexecCombopConfigVo.getExecuteConfig();
        if (executeConfigVo != null) {
            String whenToSpecify = executeConfigVo.getWhenToSpecify();
            if (CombopNodeSpecify.RUNTIME.getValue().equals(whenToSpecify)) {
                JSONArray executeParamList = new JSONArray();
                JSONObject executeNode = new JSONObject();
                executeNode.put("key", "executeNodeConfig");
                executeNode.put("name", "执行目标");
                executeNode.put("mappingMode", "");
                executeNode.put("value", "");
                executeNode.put("isRequired", 1);
                executeParamList.add(executeNode);
                JSONObject protocol = new JSONObject();
                protocol.put("key", "protocolId");
                protocol.put("name", "连接协议");
                protocol.put("mappingMode", "");
                protocol.put("value", "");
                protocol.put("isRequired", 1);
                executeParamList.add(protocol);
                JSONObject executeUser = new JSONObject();
                executeUser.put("key", "executeUser");
                executeUser.put("name", "执行用户");
                executeUser.put("mappingMode", "");
                executeUser.put("value", "");
                executeUser.put("isRequired", 1);
                executeParamList.add(executeUser);
                resultObj.put("executeParamList", executeParamList);
            }
        }
        return resultObj;
    }
}
