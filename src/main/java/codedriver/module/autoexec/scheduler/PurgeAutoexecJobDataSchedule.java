/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.scheduler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.scheduler.annotation.Input;
import codedriver.framework.scheduler.annotation.Param;
import codedriver.framework.scheduler.core.PublicJobBase;
import codedriver.framework.scheduler.dao.mapper.SchedulerMapper;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.dto.JobPropVo;
import codedriver.framework.scheduler.dto.JobVo;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Service
@Transactional
@DisallowConcurrentExecution
public class PurgeAutoexecJobDataSchedule extends PublicJobBase {
    private final static Logger logger = LoggerFactory.getLogger(PurgeAutoexecJobDataSchedule.class);
    @Resource
    SchedulerMapper schedulerMapper;
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;
    @Resource
    RunnerMapper runnerMapper;


    @Override
    public String getName() {
        return "清除自动化作业";
    }

    @Input({
            @Param(name = "expiredDays", dataType = "int", controlValue = "100", description = "天数，只保留该天数之内的作业"),
    })
    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        JobDetail jobDetail = context.getJobDetail();
        int days = 100;
        String jobUuid = jobDetail.getKey().getName();
        JobVo jobVo = schedulerMapper.getJobByUuid(jobUuid);
        if (jobVo != null) {
            //获取作业入参
            List<JobPropVo> propVoList = jobVo.getPropList();
            if (CollectionUtils.isNotEmpty(propVoList)) {
                Optional<JobPropVo> propVoOptional = propVoList.stream().filter(p -> Objects.equals(p.getName(), "expiredDays")).findFirst();
                if (propVoOptional.isPresent()) {
                    days = Integer.parseInt(propVoOptional.get().getValue());
                }
            }
            //循环调用runner 端 autoexec job 清除命令
            List<AutoexecJobVo> autoexecJobVos = autoexecJobMapper.getJobByExpiredDays(days);
            if (CollectionUtils.isNotEmpty(autoexecJobVos)) {
                List<Long> runnerMapIdList = autoexecJobMapper.getJobPhaseRunnerMapIdListByJobIdList(autoexecJobVos.stream().map(AutoexecJobVo::getId).collect(Collectors.toList()));
                List<RunnerMapVo> runnerVos = runnerMapper.getRunnerByRunnerMapIdList(runnerMapIdList);
                if (CollectionUtils.isNotEmpty(runnerVos)) {
                    runnerVos = runnerVos.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getId))), ArrayList::new));
                    UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
                    UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
                    for (RunnerMapVo runner : runnerVos) {
                        String url = runner.getUrl() + "api/rest/job/data/purge";
                        try {
                            JSONObject paramJson = new JSONObject();
                            paramJson.put("expiredDays", days);
                            paramJson.put("passThroughEnv", new JSONObject() {{
                                put("runnerId", runner.getRunnerMapId());
                            }});
                            JSONObject resultJson = HttpRequestUtil.post(url).setAuthType(AuthenticateType.BUILDIN).setPayload(paramJson.toJSONString()).sendRequest().getResultJson();
                            if (MapUtils.isEmpty(resultJson) || !resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                                logger.debug("清除作业异常："+url + ":" + resultJson.getString("Message"));
                            }
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                    //删除超时的自定化作业
                    for (AutoexecJobVo autoexecJobVo : autoexecJobVos) {
                        autoexecJobService.deleteJob(autoexecJobVo.getId());
                    }
                }
            }
        }

    }
}
