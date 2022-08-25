/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词(节点名称或ip)", xss = true),
            @Param(name = "isDelete", type = ApiParamType.STRING, desc = "是否删除"),
            @Param(name = "nodeIdList", type = ApiParamType.JSONARRAY, desc = "作业阶段节点idList,用于刷新节点"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
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
        List<AutoexecJobPhaseNodeVo> list = null;
        int rowNum = autoexecJobMapper.searchJobPhaseNodeCount(searchVo);
        searchVo.setRowNum(rowNum);
        if (rowNum > 0) {
            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30)
                    .addSheet("数据")
                    .withHeaderList(headList)
                    .withColumnList(columnList);
            Workbook workbook = builder.build();
            searchVo.setPageSize(100);
            Integer pageCount = searchVo.getPageCount();
            for (int i = 1; i <= pageCount; i++) {
                searchVo.setCurrentPage(i);
                list = autoexecJobMapper.searchJobPhaseNodeWithResource(searchVo);
                for (AutoexecJobPhaseNodeVo vo : list) {
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("host", vo.getHost());
                    dataMap.put("nodeName", vo.getNodeName());
                    dataMap.put("statusName", vo.getStatusName());
                    dataMap.put("costTime", vo.getCostTime());
                    dataMap.put("startTime", vo.getStartTime() != null ? TimeUtil.convertDateToString(vo.getStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
                    dataMap.put("endTime", vo.getEndTime() != null ? TimeUtil.convertDateToString(vo.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
                    sheetBuilder.addData(dataMap);
                }
            }

            String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), jobVo.getName() + "-" + phaseVo.getName() + ".xlsx");
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return null;
    }

    static List<String> headList = new ArrayList<>();

    static List<String> columnList = new ArrayList<>();

    static {
        headList.add("IP");
        headList.add("节点名称");
        headList.add("状态");
        headList.add("耗时");
        headList.add("开始时间");
        headList.add("结束时间");

        columnList.add("host");
        columnList.add("nodeName");
        columnList.add("statusName");
        columnList.add("costTime");
        columnList.add("startTime");
        columnList.add("endTime");
    }

}
