/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.job.AutoexecJobPhaseNodeExportHandlerFactory;
import codedriver.framework.autoexec.job.IAutoexecJobPhaseNodeExportHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import com.alibaba.fastjson.JSONObject;
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
        return "导出作业剧本节点";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "作业剧本id", isRequired = true),
    })
    @Description(desc = "导出作业剧本节点")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        AutoexecJobPhaseNodeVo searchVo = JSONObject.toJavaObject(paramObj, AutoexecJobPhaseNodeVo.class);
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(searchVo.getJobPhaseId());
        if (phaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(searchVo.getJobPhaseId().toString());
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(phaseVo.getJobId().toString());
        }
        searchVo.setJobId(jobVo.getId());
        IAutoexecJobPhaseNodeExportHandler handler = AutoexecJobPhaseNodeExportHandlerFactory.getHandler(phaseVo.getExecMode());
        if (handler != null) {
            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30);
            handler.exportJobPhaseNodeWithNodeLog(builder, searchVo, jobVo, phaseVo, getHeadList(phaseVo.getExecMode()), getColumnList(phaseVo.getExecMode()));
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
            headList.add("文件名");
        }
        headList.add("IP");
        headList.add("节点名称");
        headList.add("状态");
        headList.add("耗时");
        headList.add("开始时间");
        headList.add("结束时间");
        headList.add("执行代理");
        headList.add("日志");
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
