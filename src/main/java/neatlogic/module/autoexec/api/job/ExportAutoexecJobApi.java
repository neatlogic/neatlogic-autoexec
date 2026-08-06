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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportAutoexecJobApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(ExportAutoexecJobApi.class);

    @Resource
    AutoexecJobMapper autoexecJobMapper;

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
                    handler.exportJobPhaseNodeWithNodeOutputParam(jobVo, phaseVo, phaseOutputParamMap.get(phaseVo.getName()), builder, getHeadList(phaseVo.getExecMode()), getColumnList(phaseVo.getExecMode()));
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
