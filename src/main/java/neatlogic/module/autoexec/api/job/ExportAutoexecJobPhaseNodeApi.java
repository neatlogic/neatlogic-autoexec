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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
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
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportAutoexecJobPhaseNodeApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(ExportAutoexecJobPhaseNodeApi.class);

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/export";
    }

    @Override
    public String getName() {
        return "nmaa.exportautoexecjobphasenodeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "term.autoexec.jobphaseid", isRequired = true),
    })
    @Description(desc = "nmaa.exportautoexecjobphasenodeapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobPhaseId = paramObj.getLong("jobPhaseId");
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseId);
        if (phaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobPhaseId.toString());
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(phaseVo.getJobId().toString());
        }
        IAutoexecJobPhaseNodeExportHandler handler = AutoexecJobPhaseNodeExportHandlerFactory.getHandler(phaseVo.getExecMode());
        if (handler != null) {
            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30);
            handler.exportJobPhaseNodeWithNodeLog(jobVo, phaseVo, builder, getHeadList(phaseVo.getExecMode()), getColumnList(phaseVo.getExecMode()));
            Workbook workbook = builder.build();
            if (workbook != null) {
                String fileName = FileUtil.getEncodedFileName(jobVo.getName() + "-" + phaseVo.getName() + ".xlsx");
                response.setContentType("application/vnd.ms-excel;charset=utf-8");
                response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

                try (OutputStream os = response.getOutputStream()) {
                    workbook.write(os);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    if (workbook != null) {
                        ((SXSSFWorkbook) workbook).dispose();
                    }
                }
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
        headList.add($.t("nmar.export.log"));
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
        columnList.add("log");
        return columnList;
    }

}
