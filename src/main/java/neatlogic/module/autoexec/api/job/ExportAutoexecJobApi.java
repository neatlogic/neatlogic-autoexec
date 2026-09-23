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

package neatlogic.module.autoexec.api.job;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.OutputParamType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.AutoexecJobOutputParamExportConfig;
import neatlogic.framework.autoexec.job.AutoexecJobPhaseNodeExportHandlerFactory;
import neatlogic.framework.autoexec.job.IAutoexecJobPhaseNodeExportHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.excel.ExcelBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportAutoexecJobApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(ExportAutoexecJobApi.class);

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public String getToken() {
        return "autoexec/job/export";
    }

    @Override
    public String getName() {
        return "nmaa.exportautoexecjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
    })
    @Description(desc = "nmaa.exportautoexecjobapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        // 查询是否存在作业报告导出字段，如果存在则按<阶段名 -> <工具名 -> 字段列表>>分类
        List<JSONObject> outputDescList = mongoTemplate.find(new Query(Criteria.where("jobId").is(jobId.toString())), JSONObject.class, "_job_output_desc");
        Map<String, Map<String, List<String>>> phaseOutputParamMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(outputDescList)) {
            for (JSONObject object : outputDescList) {
                String phase = object.getString("phase");
                String pluginId = object.getString("pluginId");
                JSONArray field = object.getJSONArray("field");
                if (StringUtils.isNotBlank(phase) && StringUtils.isNotBlank(pluginId) && CollectionUtils.isNotEmpty(field)) {
                    phaseOutputParamMap.computeIfAbsent(phase, k -> new HashMap<>()).computeIfAbsent(pluginId, k -> new ArrayList<>()).addAll(field.toJavaList(String.class));
                }
            }
        }
        List<AutoexecJobPhaseVo> phaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobId);
        if (!phaseVoList.isEmpty()) {
            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30);
            for (AutoexecJobPhaseVo phaseVo : phaseVoList) {
                IAutoexecJobPhaseNodeExportHandler handler = AutoexecJobPhaseNodeExportHandlerFactory.getHandler(phaseVo.getExecMode());
                if (handler != null) {
                    Map<String, AutoexecJobOutputParamExportConfig> outputParamConfigMap = getOutputParamConfigMap(jobId, phaseVo.getId(), phaseOutputParamMap.get(phaseVo.getName()));
                    handler.exportJobPhaseNodeWithNodeOutputParam(jobVo, phaseVo, outputParamConfigMap, builder, getHeadList(phaseVo.getExecMode()), getColumnList(phaseVo.getExecMode()));
                }
            }
            try (Workbook workbook = builder.build();
                 OutputStream os = response.getOutputStream()) {
                if (workbook != null) {
                    String fileName = FileUtil.getEncodedFileName(jobVo.getName() + ".xlsx");
                    response.setContentType("application/vnd.ms-excel;charset=utf-8");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    workbook.write(os);
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 生成当前阶段的输出参数导出配置；显式报告字段优先，未配置时导出阶段全部实际输出。
     */
    Map<String, AutoexecJobOutputParamExportConfig> getOutputParamConfigMap(Long jobId, Long phaseId, Map<String, List<String>> configuredOutputParamMap) {
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationListByJobIdAndPhaseId(jobId, phaseId);
        if (CollectionUtils.isEmpty(operationVoList)) {
            return Collections.emptyMap();
        }
        boolean hasConfiguredOutputParam = MapUtils.isNotEmpty(configuredOutputParamMap);
        List<String> toolParamHashList = operationVoList.stream()
                .filter(operationVo -> CombopOperationType.TOOL.getValue().equals(operationVo.getType()))
                .map(AutoexecJobPhaseOperationVo::getParamHash)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> toolParamContentMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolParamHashList)) {
            List<AutoexecJobContentVo> contentVoList = autoexecJobMapper.getJobContentList(toolParamHashList);
            if (CollectionUtils.isNotEmpty(contentVoList)) {
                for (AutoexecJobContentVo contentVo : contentVoList) {
                    toolParamContentMap.put(contentVo.getHash(), contentVo.getContent());
                }
            }
        }
        Map<Long, Set<String>> scriptPasswordParamKeyMap = new HashMap<>();
        Map<String, AutoexecJobOutputParamExportConfig> resultMap = new HashMap<>();
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            String operationKey = operationVo.getName() + "_" + operationVo.getId();
            if (hasConfiguredOutputParam && !configuredOutputParamMap.containsKey(operationKey)) {
                continue;
            }
            Set<String> passwordParamKeySet = getPasswordParamKeySet(operationVo, toolParamContentMap, scriptPasswordParamKeyMap);
            List<String> includedParamKeyList = hasConfiguredOutputParam ? configuredOutputParamMap.get(operationKey) : Collections.emptyList();
            resultMap.put(operationKey, new AutoexecJobOutputParamExportConfig(!hasConfiguredOutputParam, includedParamKeyList, passwordParamKeySet));
        }
        return resultMap;
    }

    /**
     * 从作业快照中识别工具或脚本操作的密码类型输出参数。
     */
    private Set<String> getPasswordParamKeySet(AutoexecJobPhaseOperationVo operationVo, Map<String, String> toolParamContentMap, Map<Long, Set<String>> scriptPasswordParamKeyMap) {
        Set<String> passwordParamKeySet = new HashSet<>();
        if (CombopOperationType.TOOL.getValue().equals(operationVo.getType())) {
            String content = toolParamContentMap.get(operationVo.getParamHash());
            if (StringUtils.isNotBlank(content)) {
                JSONArray outputParamList = JSON.parseObject(content).getJSONArray("outputParamList");
                if (CollectionUtils.isNotEmpty(outputParamList)) {
                    for (Object outputParam : outputParamList) {
                        JSONObject outputParamJson = JSON.parseObject(outputParam.toString());
                        if (OutputParamType.PASSWORD.getValue().equals(outputParamJson.getString("type"))) {
                            passwordParamKeySet.add(outputParamJson.getString("key"));
                        }
                    }
                }
            }
        } else if (CombopOperationType.SCRIPT.getValue().equals(operationVo.getType()) && operationVo.getVersionId() != null) {
            return scriptPasswordParamKeyMap.computeIfAbsent(operationVo.getVersionId(), versionId -> {
                Set<String> keySet = new HashSet<>();
                List<AutoexecScriptVersionParamVo> paramVoList = autoexecScriptMapper.getOutputParamListByVersionId(versionId);
                if (CollectionUtils.isNotEmpty(paramVoList)) {
                    paramVoList.stream()
                            .filter(paramVo -> OutputParamType.PASSWORD.getValue().equals(paramVo.getType()))
                            .map(AutoexecScriptVersionParamVo::getKey)
                            .forEach(keySet::add);
                }
                return keySet;
            });
        }
        return passwordParamKeySet;
    }

    private List<String> getHeadList(String execMode) {
        List<String> headList = new ArrayList<>();
        if (ExecMode.SQL.getValue().equals(execMode)) {
            headList.add($.t("nmar.export.filename"));
        }
        headList.add("IP");
        headList.add($.t("term.autoexec.nodename"));
        headList.add($.t("common.status"));
        headList.add($.t("common.timecost"));
        headList.add($.t("common.starttime"));
        headList.add($.t("common.endtime"));
        headList.add($.t("nmar.export.runner"));
        headList.add($.t("nmar.export.outputparameters"));
        return headList;
    }

    private List<String> getColumnList(String execMode) {
        List<String> columnList = new ArrayList<>();
        if (ExecMode.SQL.getValue().equals(execMode)) {
            columnList.add("name");
        }
        columnList.add("host");
        columnList.add("nodeName");
        columnList.add("statusName");
        columnList.add("costTime");
        columnList.add("startTime");
        columnList.add("endTime");
        columnList.add("runner");
        columnList.add("outputParam");
        return columnList;
    }

}
