/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.autoconfig.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auditconfig.core.AuditCleanerBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.healthcheck.dao.mapper.DatabaseFragmentMapper;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.$;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Component
public class AutoexecJobCleaner extends AuditCleanerBase {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobCleaner.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    private DatabaseFragmentMapper databaseFragmentMapper;

    @Override
    public String getName() {
        return "AUTOEXEC-JOB";
    }

    @Override
    protected void myClean(int dayBefore) throws Exception {
        //循环调用runner 端 autoexec job 清除命令
        List<AutoexecJobVo> autoexecJobVos = autoexecJobMapper.getJobByExpiredDays(dayBefore);
        if (CollectionUtils.isNotEmpty(autoexecJobVos)) {
            List<Long> runnerMapIdList = autoexecJobMapper.getJobPhaseRunnerMapIdListByJobIdList(autoexecJobVos.stream().map(AutoexecJobVo::getId).collect(Collectors.toList()));
            List<RunnerMapVo> runnerVos = runnerMapper.getRunnerByRunnerMapIdList(runnerMapIdList);
            if (CollectionUtils.isNotEmpty(runnerVos)) {
                runnerVos = runnerVos.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getId))), ArrayList::new));
                UserContext.init(SystemUser.SYSTEM);
                UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
                for (RunnerMapVo runner : runnerVos) {
                    String url = runner.getUrl() + "api/rest/job/data/purge";
                    try {
                        JSONObject paramJson = new JSONObject();
                        paramJson.put("expiredDays", dayBefore);
                        paramJson.put("passThroughEnv", new JSONObject() {{
                            put("runnerId", runner.getRunnerMapId());
                        }});
                        JSONObject resultJson = HttpRequestUtil.post(url).setConnectTimeout(5000).setReadTimeout(10000).setAuthType(AuthenticateType.BUILDIN).setPayload(paramJson.toJSONString()).sendRequest().getResultJson();
                        if (MapUtils.isEmpty(resultJson) || !resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                            String errorMsg = $.t("nmaah.autoexecjobcleaner.myclean.error", url, (MapUtils.isEmpty(resultJson) ? StringUtils.EMPTY : resultJson.toJSONString()));
                            logger.error(errorMsg);
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                //删除超时的自定化作业
                for (AutoexecJobVo autoexecJobVo : autoexecJobVos) {
                    autoexecJobService.deleteJob(autoexecJobVo);
                }
            }
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_phase");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_phase_node");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_phase_operation");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_phase_node_runner");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_phase_runner");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_resource_inspect");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_invoke");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_group");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_env");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "autoexec_job_sql_detail");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "deploy_sql_detail");
        }
    }

}
