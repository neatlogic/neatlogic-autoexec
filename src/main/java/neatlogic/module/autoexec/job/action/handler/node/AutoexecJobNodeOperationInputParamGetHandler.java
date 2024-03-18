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

package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dto.AutoexecOperationBaseVo;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/11/23 12:18
 **/
@Service
public class AutoexecJobNodeOperationInputParamGetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeOperationInputParamGetHandler.class);
    @Resource
    AutoexecJobService autoexecJobService;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return JobAction.GET_NODE_OPERATION_INPUT_PARAM.getValue();
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
        JSONArray operationInputParamList = new JSONArray();
        result.put("operationInputParamArray", operationInputParamList);
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramJson = jobVo.getActionParam();
        paramJson.put("phase", nodeVo.getJobPhaseName());
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhase().getId());
        paramJson.put("execMode", jobVo.getCurrentPhase().getExecMode());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        String url = nodeVo.getRunnerUrl() + "/api/rest/job/phase/node/input/param/get";
        JSONObject paramsJson = JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));

        //批量查找入参
        List<AutoexecOperationVo> operationVos = new ArrayList<>();
        List<AutoexecOperationVo> argumentsOperationVos = new ArrayList<>();
        List<AutoexecJobPhaseOperationVo> scriptList = new ArrayList<>();
        List<Long> toolIdList = new ArrayList<>();
        for (AutoexecJobPhaseOperationVo jobPhaseOperationVo : operationVoList) {
            if (Objects.equals(CombopOperationType.SCRIPT.getValue(), jobPhaseOperationVo.getType())) {
                scriptList.add(jobPhaseOperationVo);
            } else {
                toolIdList.add(jobPhaseOperationVo.getOperationId());
            }
        }
        if (CollectionUtils.isNotEmpty(scriptList)) {
            operationVos.addAll(autoexecScriptMapper.getAutoexecOperationInputParamList(scriptList));
            argumentsOperationVos.addAll(autoexecScriptMapper.getArgumentListByScriptIdList(scriptList.stream().map(AutoexecJobPhaseOperationVo::getOperationId).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            operationVos.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
            argumentsOperationVos.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
        }
        Map<Long, AutoexecOperationVo> operationVoMap = operationVos.stream().collect(Collectors.toMap(AutoexecOperationBaseVo::getId, o -> o));
        Map<Long, AutoexecOperationVo> argumentsOperationVoMap = argumentsOperationVos.stream().collect(Collectors.toMap(AutoexecOperationBaseVo::getId, o -> o));
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            JSONObject paramOperation = paramsJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
            JSONObject inputParams = null;
            JSONArray arguments = null;
            if (MapUtils.isNotEmpty(paramOperation)) {
                inputParams = paramOperation.getJSONObject("options");
                arguments = paramOperation.getJSONArray("arguments");
            }
            JSONArray paramList = new JSONArray();
            JSONObject operationParam = new JSONObject();
            JSONObject argument = null;
            operationParam.put("name", operationVo.getName());
            operationParam.put("letter", operationVo.getLetter());
            operationParam.put("paramList", paramList);
            //arguments
            if (argumentsOperationVoMap.containsKey(operationVo.getOperationId())) {
                AutoexecParamVo argumentParam = argumentsOperationVoMap.get(operationVo.getOperationId()).getArgument();
                argument = new JSONObject();
                if (argumentParam != null) {
                    argument.put("name", argumentParam.getName());
                    argument.put("description", argumentParam.getDescription());
                }
                argument.put("key", "arguments");
                if (CollectionUtils.isNotEmpty(arguments)) {
                    argument.put("valueList", arguments.stream().map(o -> o instanceof JSONObject ? JSON.parseObject(o.toString()).getString("value") : o.toString()).collect(Collectors.toList()));
                }
                operationParam.put("argument", argument);
            }
            //input
            if (operationVoMap.containsKey(operationVo.getOperationId())) {
                List<AutoexecParamVo> inputParamList = operationVoMap.get(operationVo.getOperationId()).getInputParamList();
                if (CollectionUtils.isNotEmpty(inputParamList)) {
                    for (AutoexecParamVo paramVo : inputParamList) {
                        JSONObject param = new JSONObject();
                        param.put("name", paramVo.getName());
                        param.put("key", paramVo.getKey());
                        if (MapUtils.isNotEmpty(inputParams)) {
                            if (Objects.equals(paramVo.getType(), ParamType.FILE.getValue())) {
                                Object value = inputParams.get(paramVo.getKey());
                                if (value != null) {
                                    String valueStr = value.toString().replace("file/", StringUtils.EMPTY);
                                    try {
                                        param.put("value", JSON.parseArray(URLDecoder.decode(valueStr, StandardCharsets.UTF_8.toString())));
                                    } catch (Exception ignored) {
                                        param.put("value", value);
                                    }
                                }
                            } else {
                                param.put("value", inputParams.get(paramVo.getKey()));
                            }
                        }
                        param.put("description", paramVo.getDescription());
                        param.put("type", paramVo.getType());
                        paramList.add(param);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(paramList) || MapUtils.isNotEmpty(argument)) {
                operationInputParamList.add(operationParam);
            }
        }
        return result;
    }
}
