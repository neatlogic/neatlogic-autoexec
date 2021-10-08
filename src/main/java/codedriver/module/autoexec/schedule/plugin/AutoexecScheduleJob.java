/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author linbq
 * @since 2021/9/29 17:42
 **/
@Component
@DisallowConcurrentExecution
public class AutoexecScheduleJob extends JobBase {

    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-AUTOEXEC-SCHEDULE-JOB";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        AutoexecScheduleVo autoexecScheduleVo = (AutoexecScheduleVo) jobObject.getData("autoexecScheduleVo");
        JobObject newJobObjectBuilder = new JobObject.Builder(autoexecScheduleVo.getUuid(), this.getGroupName(), this.getClassName(), tenantUuid)
                .withCron(autoexecScheduleVo.getCron()).withBeginTime(autoexecScheduleVo.getBeginTime())
                .withEndTime(autoexecScheduleVo.getEndTime())
//                .needAudit(autoexecScheduleVo.getNeedAudit())
                .setType("private")
                .build();
        schedulerManager.loadJob(newJobObjectBuilder);
    }

    @Override
    public void initJob(String tenantUuid) {
        AutoexecScheduleVo searchVo = new AutoexecScheduleVo();
        searchVo.setIsActive(1);
        int rowNum = autoexecScheduleMapper.getAutoexecScheduleCount(searchVo);
        searchVo.setPageSize(100);
        for (int currentPage = 1; rowNum > 0; currentPage++, rowNum -= 100) {
            searchVo.setCurrentPage(currentPage);
            List<AutoexecScheduleVo> autoexecScheduleList = autoexecScheduleMapper.getAutoexecScheduleList(searchVo);
            for (AutoexecScheduleVo autoexecScheduleVo : autoexecScheduleList) {
                JobObject.Builder jobObjectBuilder = new JobObject
                        .Builder(autoexecScheduleVo.getUuid(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid())
                        .addData("autoexecScheduleVo", autoexecScheduleVo);
                JobObject jobObject = jobObjectBuilder.build();
                this.reloadJob(jobObject);
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        String uuid = jobObject.getJobName();
        AutoexecScheduleVo autoexecScheduleVo = autoexecScheduleMapper.getAutoexecScheduleByUuid(uuid);
        System.out.println(new Date() + "执行定时作业：'" + autoexecScheduleVo.getName() + "'");
        JSONObject paramObj = new JSONObject();
//        Long combopId = autoexecConfig.getLong("autoexecCombopId");
        paramObj.put("combopId", autoexecScheduleVo.getAutoexecCombopId());
//        JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
//        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
//            JSONObject param = new JSONObject();
//            for (int i = 0; i < runtimeParamList.size(); i++) {
//                JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
//                if (MapUtils.isNotEmpty(runtimeParamObj)) {
//                    String key = runtimeParamObj.getString("key");
//                    if (StringUtils.isNotBlank(key)) {
//                        String value = runtimeParamObj.getString("value");
//                        if (StringUtils.isNotBlank(value)) {
//                            String mappingMode = runtimeParamObj.getString("mappingMode");
//                            param.put(key, parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
//                        } else {
//                            param.put(key, value);
//                        }
//                    }
//                }
//            }
//            paramObj.put("param", param);
//        }
//        JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
//        if (CollectionUtils.isNotEmpty(executeParamList)) {
//            JSONObject executeConfig = new JSONObject();
//            for (int i = 0; i < executeParamList.size(); i++) {
//                JSONObject executeParamObj = executeParamList.getJSONObject(i);
//                if (MapUtils.isNotEmpty(executeParamObj)) {
//                    String key = executeParamObj.getString("key");
//                    String value = executeParamObj.getString("value");
//                    String mappingMode = executeParamObj.getString("mappingMode");
//                    if ("protocol".equals(key)) {
//                        if (StringUtils.isNotBlank(value)) {
//                            executeConfig.put("protocol", parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
//                        } else {
//                            executeConfig.put("protocol", value);
//                        }
//                    } else if ("executeUser".equals(key)) {
//                        if (StringUtils.isNotBlank(value)) {
//                            executeConfig.put("executeUser", parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
//                        } else {
//                            executeConfig.put("executeUser", value);
//                        }
//                    } else if ("executeNodeConfig".equals(key)) {
//                        if (StringUtils.isNotBlank(value)) {
//                            executeConfig.put("executeNodeConfig", parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
//                        } else {
//                            executeConfig.put("executeNodeConfig", value);
//                        }
//                    }
//                }
//            }
//            paramObj.put("executeConfig", executeConfig);
//        }
        paramObj.put("source", "autoexecSchedule");
        paramObj.put("threadCount", 1);
        paramObj.put("invokeId", autoexecScheduleVo.getId());
//        AutoexecJobVo jobVo = autoexecJobActionService.validateCreateJobFromCombop(paramObj, false);
//        autoexecJobActionService.fire(jobVo);
    }
}
