/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.job.*;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeOutputParamGetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeOutputParamGetHandler.class);
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.GET_NODE_OUTPUT_PARAM.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getExecutePhase();
        JSONObject paramJson = jobVo.getActionParam();
        paramJson.put("jobId", nodeVo.getJobId());
        paramJson.put("phase", nodeVo.getJobPhaseName());
        paramJson.put("nodeId", nodeVo.getId());
        paramJson.put("resourceId", nodeVo.getResourceId());
        paramJson.put("phaseId", nodeVo.getJobPhaseId());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        paramJson.put("runnerUrl", nodeVo.getRunnerUrl());
        paramJson.put("execMode", phaseVo.getExecMode());
        JSONArray operationOutputParamArray = null;
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/output/param/get";
        JSONObject statusJson = JSON.parseObject(AutoexecUtil.requestRunner(url, paramJson));
        if (MapUtils.isNotEmpty(statusJson)) {
            Long jobId = paramJson.getLong("jobId");
            Long jobPhaseId = paramJson.getLong("phaseId");
            List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationListWithVersionAndParentByJobIdAndPhaseId(jobId, jobPhaseId);
            List<String> toolContentList = operationVoList.stream().filter(o -> Objects.equals(o.getType(), CombopOperationType.TOOL.getValue())).map(AutoexecJobPhaseOperationVo::getParamHash).collect(Collectors.toList());
            Map<String, String> toolHashContentMap = new HashMap<>();
            if(CollectionUtils.isNotEmpty(toolContentList)) {
                List<AutoexecJobContentVo> toolParamContentVoList = autoexecJobMapper.getJobContentList(toolContentList);
                toolHashContentMap = toolParamContentVoList.stream().collect(Collectors.toMap(AutoexecJobContentVo::getHash, AutoexecJobContentVo::getContent));
            }
            AutoexecJobPhaseNodeVo phaseNodeVo = autoexecJobService.getNodeOperationStatus(paramJson, true);
            List<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVos = phaseNodeVo.getOperationStatusVoList();
            Map<String, String> finalToolHashContentMap = toolHashContentMap;
            operationOutputParamArray = new JSONArray() {{
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    add(new JSONObject() {{
                        put("name", operationVo.getName());
                        JSONObject valueJson = statusJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
                        List<AutoexecParamVo> outputParamList = new ArrayList<>();
                        if (Objects.equals(operationVo.getType(), CombopOperationType.TOOL.getValue()) && finalToolHashContentMap.containsKey(operationVo.getParamHash())) {
                            JSONObject json = JSON.parseObject(finalToolHashContentMap.get(operationVo.getParamHash()));
                            JSONArray outputArray = json.getJSONArray("outputParamList");
                            if(CollectionUtils.isNotEmpty(outputArray)) {
                                for (Object output : outputArray) {
                                    AutoexecParamVo outputVo = JSON.parseObject(output.toString()).toJavaObject(AutoexecParamVo.class);
                                    if (valueJson != null) {
                                        outputVo.setValue(valueJson.getString(outputVo.getKey()));
                                    }
                                    outputParamList.add(outputVo);
                                }
                            }
                        }else if(Objects.equals(operationVo.getType(), CombopOperationType.SCRIPT.getValue())){
                            List<AutoexecScriptVersionParamVo> scriptVersionParamVos = autoexecScriptMapper.getOutputParamListByVersionId(operationVo.getVersionId());
                            for(AutoexecScriptVersionParamVo scriptVersionParamVo : scriptVersionParamVos) {
                                AutoexecParamVo outputVo = new AutoexecParamVo(scriptVersionParamVo);
                                if (valueJson != null) {
                                    outputVo.setValue(valueJson.getString(outputVo.getKey()));
                                }
                                outputParamList.add(outputVo);
                            }
                        }
                        outputParamList = outputParamList.stream().sorted(Comparator.comparing(AutoexecParamVo::getSort)).collect(Collectors.toList());
                        put("paramList", outputParamList);
                        Optional<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVoOptional = operationStatusVos.stream().filter(o -> Objects.equals(o.getName(), operationVo.getName())).findFirst();
                        operationStatusVoOptional.ifPresent(autoexecJobPhaseNodeOperationStatusVo -> put("status", autoexecJobPhaseNodeOperationStatusVo.getStatus()));
                    }});
                }
            }};
        }

        result.put("operationOutputParamArray", operationOutputParamArray);
        return result;
    }
}
