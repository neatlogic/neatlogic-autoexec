package codedriver.module.autoexec.job.source.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/5/31 2:34 下午
 */
@Service
public class AutoexecJobSourceHandler extends AutoexecJobSourceActionHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Override
    public String getName() {
        return AutoexecOperType.AUTOEXEC.getValue();
    }

    @Override
    public void saveJobPhase(AutoexecCombopPhaseVo combopPhaseVo) {

    }

    @Override
    public String getJobSqlContent(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        return AutoexecUtil.requestRunner(nodeVo.getRunnerUrl() + "/api/rest/job/phase/node/sql/content/get", paramObj);
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
            throw new AutoexecJobRunnerHttpRequestException(url + ":" + result);
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

}
