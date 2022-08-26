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
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

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
            searchVo.setPageSize(20);
            Integer pageCount = searchVo.getPageCount();
            for (int i = 1; i <= pageCount; i++) {
                searchVo.setCurrentPage(i);
                list = autoexecJobMapper.searchJobPhaseNodeWithResource(searchVo);
                Map<Long, Map<String, Object>> nodeDataMap = new LinkedHashMap<>();
                Map<String, List<Long>> runnerNodeMap = new HashMap<>();
                Map<Long, JSONObject> nodeLogTailParamMap = new HashMap<>();
                for (AutoexecJobPhaseNodeVo vo : list) {
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("host", vo.getHost());
                    dataMap.put("nodeName", vo.getNodeName());
                    dataMap.put("statusName", vo.getStatusName());
                    dataMap.put("costTime", vo.getCostTime());
                    dataMap.put("startTime", vo.getStartTime() != null ? TimeUtil.convertDateToString(vo.getStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
                    dataMap.put("endTime", vo.getEndTime() != null ? TimeUtil.convertDateToString(vo.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
                    nodeDataMap.put(vo.getId(), dataMap);
                    runnerNodeMap.computeIfAbsent(vo.getRunnerUrl(), k -> new ArrayList<>()).add(vo.getId());
                    nodeLogTailParamMap.put(vo.getId(), new JSONObject() {
                        {
                            this.put("id", vo.getId());
                            this.put("jobId", jobVo.getId());
                            this.put("resourceId", vo.getResourceId());
                            this.put("phase", phaseVo.getName());
                            this.put("ip", vo.getHost());
                            this.put("port", vo.getPort());
                            this.put("execMode", phaseVo.getExecMode());
                        }
                    });
                }
                for (Map.Entry<String, List<Long>> entry : runnerNodeMap.entrySet()) {
                    String url = entry.getKey() + "api/binary/job/phase/node/log/tail/withlimit";
                    List<Long> value = entry.getValue();
                    if (CollectionUtils.isNotEmpty(value)) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        JSONArray nodeList = new JSONArray();
                        value.forEach(o -> nodeList.add(nodeLogTailParamMap.get(o)));
                        JSONObject paramJson = new JSONObject();
                        paramJson.put("nodeList", nodeList);
                        paramJson.put("charLimit", 2048);
                        HttpRequestUtil requestUtil = HttpRequestUtil.download(url, "POST", bos)
                                .setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
                        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                        InputStreamReader isr = new InputStreamReader(bis);
                        JSONReader jsonReader = new JSONReader(isr);
                        jsonReader.startArray();
                        while (jsonReader.hasNext()) {
                            JSONObject nodeObj = jsonReader.readObject(JSONObject.class);
                            Long id = nodeObj.getLong("id");
                            String content = nodeObj.getString("content");
                            Map<String, Object> map = nodeDataMap.get(id);
                            if (map != null) {
                                map.put("log", content);
                            }
                        }
                        jsonReader.endArray();
                        jsonReader.close();
                        bis.close();
                        bos.close();
                    }
                }
                nodeDataMap.values().forEach(sheetBuilder::addData);
            }

            String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), jobVo.getName() + "-" + phaseVo.getName() + ".xlsx");
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
        headList.add("日志");

        columnList.add("host");
        columnList.add("nodeName");
        columnList.add("statusName");
        columnList.add("costTime");
        columnList.add("startTime");
        columnList.add("endTime");
        columnList.add("log");
    }

}
