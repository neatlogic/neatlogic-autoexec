package codedriver.module.autoexec.job.source.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.framework.common.util.IpUtil;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.dto.runner.GroupNetworkVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import codedriver.framework.exception.runner.RunnerHttpRequestException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/5/31 2:34 下午
 */
@Service
public class AutoexecJobSourceTypeHandler extends AutoexecJobSourceTypeHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return JobSourceType.AUTOEXEC.getValue();
    }

    @Override
    public void saveJobPhase(AutoexecCombopPhaseVo combopPhaseVo) {

    }

    @Override
    public JSONObject getJobSqlContent(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        return JSONObject.parseObject(AutoexecUtil.requestRunner(nodeVo.getRunnerUrl() + "/api/rest/job/phase/node/sql/content/get", paramObj));
    }

    @Override
    public void downloadJobSqlFile(AutoexecJobVo jobVo) throws Exception {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + paramObj.getString("sqlName") + "\"");
        String url = nodeVo.getRunnerUrl() + "/api/binary/job/phase/node/sql/file/download";
        String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream()).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest().getError();

        if (StringUtils.isNotBlank(result)) {
            throw new RunnerHttpRequestException(url + ":" + result);
        }
    }

    @Override
    public void resetSqlStatus(JSONObject paramObj, AutoexecJobVo jobVo) {
        List<AutoexecSqlDetailVo> resetSqlList = null;
        List<Long> resetSqlIdList = null;
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        if (!Objects.isNull(paramObj.getInteger("isAll")) && paramObj.getInteger("isAll") == 1) {
            //重置phase的所有sql文件状态
            resetSqlList = autoexecJobMapper.getJobSqlListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            resetSqlIdList = resetSqlList.stream().map(AutoexecSqlDetailVo::getId).collect(Collectors.toList());
            List<AutoexecJobPhaseNodeVo> jobPhaseNodeVos = new ArrayList<>();
            List<RunnerMapVo> runnerMapVos = runnerMapper.getRunnerByRunnerMapIdList(resetSqlList.stream().map(AutoexecSqlDetailVo::getRunnerId).collect(Collectors.toList()));
            for (AutoexecSqlDetailVo resetSql : resetSqlList) {
                Optional<RunnerMapVo> runnerMapVoOptional = runnerMapVos.stream().filter(o -> Objects.equals(o.getRunnerMapId(), resetSql.getRunnerId())).findFirst();
                if (!runnerMapVoOptional.isPresent()) {
                    throw new AutoexecJobRunnerNotFoundException(resetSql.getRunnerId().toString());
                }
                jobPhaseNodeVos.add(new AutoexecJobPhaseNodeVo(resetSql.getJobId(), resetSql.getPhaseName(), resetSql.getHost(), resetSql.getPort(), resetSql.getResourceId(), runnerMapVoOptional.get().getUrl(), resetSql.getRunnerId()));
            }
            jobVo.setExecuteJobNodeVoList(jobPhaseNodeVos);
        } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            resetSqlIdList = sqlIdArray.toJavaList(Long.class);
            jobVo.setExecuteJobNodeVoList(autoexecJobMapper.getJobPhaseNodeListBySqlIdList(resetSqlIdList));
        }
        //批量重置sql文件状态
        if (CollectionUtils.isNotEmpty(resetSqlIdList)) {
            autoexecJobMapper.resetJobSqlStatusBySqlIdList(resetSqlIdList);
        }
    }

    @Override
    public JSONObject searchJobPhaseSql(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<AutoexecSqlDetailVo> returnList = new ArrayList<>();
        int sqlCount = autoexecJobMapper.searchJobPhaseSqlCount(jobPhaseNodeVo);
        if (sqlCount > 0) {
            jobPhaseNodeVo.setRowNum(sqlCount);
            returnList = autoexecJobMapper.searchJobPhaseSql(jobPhaseNodeVo);
        }
        return TableResultUtil.getResult(returnList, jobPhaseNodeVo);
    }

    @Override
    public void checkinSqlList(JSONObject paramObj) {
        AutoexecJobPhaseVo targetPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(paramObj.getLong("jobId"), paramObj.getString("targetPhaseName"));
        if (targetPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(paramObj.getString("targetPhaseName"));
        }
        JSONArray paramSqlVoArray = paramObj.getJSONArray("sqlInfoList");
        Date nowLcd = new Date();
        if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
            List<AutoexecSqlDetailVo> insertSqlList = paramSqlVoArray.toJavaList(AutoexecSqlDetailVo.class);
            if (insertSqlList.size() > 100) {
                int cyclicNumber = insertSqlList.size() / 100;
                if (insertSqlList.size() % 100 != 0) {
                    cyclicNumber++;
                }
                for (int i = 0; i < cyclicNumber; i++) {
                    autoexecJobMapper.insertSqlDetailList(insertSqlList.subList(i * 100, (Math.min((i + 1) * 100, insertSqlList.size()))), targetPhaseVo.getName(), targetPhaseVo.getId(), paramObj.getLong("runnerId"), nowLcd);
                }
            } else {
                autoexecJobMapper.insertSqlDetailList(insertSqlList, targetPhaseVo.getName(), targetPhaseVo.getId(), paramObj.getLong("runnerId"), nowLcd);
            }
            List<Long> needDeleteSqlIdList = autoexecJobMapper.getSqlDetailIdListByJobIdAndPhaseNameAndResourceIdAndLcd(paramObj.getLong("jobId"), insertSqlList.get(0).getResourceId(), paramObj.getString("targetPhaseName"), nowLcd);
            if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                autoexecJobMapper.updateSqlIsDeleteByIdList(needDeleteSqlIdList);
            }
        }
    }

    @Override
    public void updateSqlStatus(JSONObject paramObj) {
        AutoexecSqlDetailVo paramSqlVo = paramObj.getJSONObject("sqlStatus").toJavaObject(AutoexecSqlDetailVo.class);
        paramSqlVo.setPhaseName(paramObj.getString("phaseName"));
        paramSqlVo.setJobId(paramObj.getLong("jobId"));
        AutoexecSqlDetailVo oldSqlDetailVo = autoexecJobMapper.getJobSqlByResourceIdAndJobIdAndJobPhaseNameAndSqlFile(paramSqlVo.getResourceId(), paramObj.getLong("jobId"), paramObj.getString("phaseName"), paramSqlVo.getSqlFile());
        if (oldSqlDetailVo == null) {
            AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            if (phaseVo == null) {
                throw new AutoexecJobPhaseNotFoundException(paramObj.getString("phaseName"));
            }
            paramSqlVo.setRunnerId(paramObj.getLong("runnerId"));
            paramSqlVo.setJobId(paramObj.getLong("jobId"));
            paramSqlVo.setPhaseId(phaseVo.getId());
            autoexecJobMapper.insertSqlDetail(paramSqlVo);
        } else {
            paramSqlVo.setId(oldSqlDetailVo.getId());
            autoexecJobMapper.updateSqlDetailById(paramSqlVo);
        }
    }

    @Override
    public AutoexecSqlDetailVo getSqlDetail(AutoexecJobVo jobVo) {
        return autoexecJobMapper.getJobSqlByJobPhaseIdAndResourceIdAndSqlName(jobVo.getActionParam().getLong("jobPhaseId"), jobVo.getActionParam().getLong("resourceId"), jobVo.getActionParam().getString("sqlName"));
    }

    @Override
    public List<RunnerMapVo> getRunnerMapList(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        List<RunnerMapVo> runnerMapVos = null;
        if (Arrays.asList(ExecMode.TARGET.getValue(), ExecMode.RUNNER_TARGET.getValue()).contains(jobPhaseVo.getExecMode())) {
            List<GroupNetworkVo> networkVoList = runnerMapper.getAllNetworkMask();
            for (GroupNetworkVo networkVo : networkVoList) {
                if (IpUtil.isBelongSegment(jobPhaseVo.getCurrentNode().getHost(), networkVo.getNetworkIp(), networkVo.getMask())) {
                    RunnerGroupVo groupVo = runnerMapper.getRunnerMapGroupById(networkVo.getGroupId());
                    if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
                        throw new RunnerGroupRunnerNotFoundException(groupVo.getName() + "(" + networkVo.getGroupId() + ") ");
                    }
                    runnerMapVos = groupVo.getRunnerMapList();
                }
            }
        } else {
            runnerMapVos = runnerMapper.getAllRunnerMap();
        }
        return runnerMapVos;
    }

    @Override
    public AutoexecCombopVo getAutoexecCombop(AutoexecJobVo autoexecJobParam) {
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecJobParam.getOperationId());
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecJobParam.getOperationId());
        }
        if (StringUtils.isBlank(autoexecJobParam.getName())) {
            autoexecJobParam.setName(combopVo.getName());
        }
        return combopVo;
    }

    @Override
    public List<AutoexecJobPhaseNodeVo> getJobNodeListBySqlIdList(List<Long> sqlIdList) {
        return autoexecJobMapper.getJobPhaseNodeListBySqlIdList(sqlIdList);
    }

    @Override
    public boolean getIsCanUpdatePhaseRunner(AutoexecJobPhaseVo jobPhaseVo, Long runnerMapId) {
        if (Objects.equals(jobPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            List<AutoexecSqlDetailVo> sqlDetail = autoexecJobMapper.getJobSqlDetailListByJobIdAndPhaseNameAndExceptStatusAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getName(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
            return sqlDetail.size() == 0;
        } else {
            List<AutoexecJobPhaseNodeVo> phaseNodes = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseIdAndExceptStatusAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getId(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
            return phaseNodes.size() == 0;
        }
    }

    @Override
    public void myExecuteAuthCheck(AutoexecJobVo jobVo) {
        //先校验有没有组合工具权限
        if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            if (combopVo == null && jobVo.getIsTakeOver() == 0) {
                throw new AutoexecCombopNotFoundException(jobVo.getOperationId());
            }
            if (combopVo != null && !Objects.equals(combopVo.getOwner(), jobVo.getExecUser()) && !autoexecCombopService.checkOperableButton(combopVo, CombopAuthorityAction.EXECUTE, jobVo.getExecUser())) {
                throw new AutoexecJobCanNotCreateException(combopVo.getName());
            }
        } else if (Arrays.asList(CombopOperationType.SCRIPT.getValue(), CombopOperationType.TOOL.getValue()).contains(jobVo.getOperationType())) {
            if (!AuthActionChecker.checkByUserUuid(jobVo.getExecUser(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                throw new AutoexecScriptJobCanNotExecuteException(jobVo.getId());
            }
        }
    }

    @Override
    public List<String> getModifyAuthList() {
        return Collections.singletonList(AUTOEXEC_JOB_MODIFY.class.getSimpleName());
    }

    @Override
    public void getJobActionAuth(AutoexecJobVo jobVo) {
        if ((Objects.equals(jobVo.getSource(), JobSource.TEST.getValue()))) {
            if (AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class)) {
                if (UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                    jobVo.setIsCanExecute(1);
                } else {
                    jobVo.setIsCanTakeOver(1);
                }
            }
        } else if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            //如果组合工具已经被删除，则只需要校验执行用户即可
            if (combopVo == null && UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                jobVo.setIsCanExecute(1);
            }
            if (combopVo != null && autoexecCombopService.checkOperableButton(combopVo, CombopAuthorityAction.EXECUTE)) {
                if (UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                    jobVo.setIsCanExecute(1);
                } else {
                    jobVo.setIsCanTakeOver(1);
                }
            }
        }

    }

    @Override
    public Workbook exportJobPhaseSql(AutoexecJobPhaseNodeVo jobPhaseNodeVo, AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo, List<String> headList, List<String> columnList) {
        Workbook workbook = null;
        int sqlCount = autoexecJobMapper.searchJobPhaseSqlCount(jobPhaseNodeVo);
        if (sqlCount > 0) {
            List<AutoexecSqlDetailVo> list;
            jobPhaseNodeVo.setRowNum(sqlCount);
            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30)
                    .addSheet("数据")
                    .withHeaderList(headList)
                    .withColumnList(columnList);
            workbook = builder.build();
            jobPhaseNodeVo.setPageSize(20);
            Integer pageCount = jobPhaseNodeVo.getPageCount();
            for (int i = 1; i <= pageCount; i++) {
                jobPhaseNodeVo.setCurrentPage(i);
                list = autoexecJobMapper.searchJobPhaseSql(jobPhaseNodeVo);
                Map<Long, Map<String, Object>> nodeDataMap = new LinkedHashMap<>();
                Map<String, List<Long>> runnerNodeMap = new HashMap<>();
                Map<Long, JSONObject> nodeLogTailParamMap = new HashMap<>();
                for (AutoexecSqlDetailVo vo : list) {
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("name", vo.getSqlFile());
                    dataMap.put("host", vo.getHost() + (vo.getPort() != null ? ":" + vo.getPort() : ""));
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
                            this.put("sqlName", vo.getSqlFile());
                            this.put("phase", phaseVo.getName());
                            this.put("ip", vo.getHost());
                            this.put("port", vo.getPort());
                            this.put("execMode", phaseVo.getExecMode());
                        }
                    });
                }
                for (Map.Entry<String, List<Long>> entry : runnerNodeMap.entrySet()) {
                    String url = entry.getKey() + "api/binary/job/phase/batchnode/log/tail";
                    List<Long> value = entry.getValue();
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(value)) {
                        JSONArray nodeList = new JSONArray();
                        value.forEach(o -> nodeList.add(nodeLogTailParamMap.get(o)));
                        JSONObject paramJson = new JSONObject();
                        paramJson.put("nodeList", nodeList);
                        paramJson.put("wordCountLimit", 2048);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        HttpRequestUtil requestUtil = HttpRequestUtil.download(url, "POST", bos)
                                .setPayload(paramJson.toJSONString())
                                .setAuthType(AuthenticateType.BUILDIN)
                                .setConnectTimeout(5000)
                                .setReadTimeout(5000)
                                .sendRequest();
                        String error = requestUtil.getError();
                        if (StringUtils.isNotBlank(error)) {
                            continue;
                        }
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
                        try {
                            bis.close();
                            bos.close();
                        } catch (IOException e) {

                        }
                    }
                }
                nodeDataMap.values().forEach(sheetBuilder::addData);
            }
        }
        return workbook;
    }
}
