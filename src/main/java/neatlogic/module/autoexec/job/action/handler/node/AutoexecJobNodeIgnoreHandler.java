/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.job.action.handler.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeIgnoreHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeResetHandler.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.IGNORE_NODE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        List<AutoexecJobPhaseNodeVo> nodeVoList;
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getExecutePhase();
        //重置mongodb node 状态
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        IAutoexecJobSourceTypeHandler handler = null;
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            handler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            handler.ignoreSql(jobVo.getActionParam(), jobVo);
        } else {
            currentResourceIdListValid(jobVo);
            nodeVoList = autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
            for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getExecuteJobNodeVoList()) {
                nodeVo.setStatus(JobNodeStatus.IGNORED.getValue());
                autoexecJobMapper.updateJobPhaseNodeById(nodeVo);
            }
            for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
                runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
            }
            runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));

            for(RunnerMapVo runnerMapVo : runnerVos){
                autoexecJobService.updatePartialNodeJobAndPhaseWithRunnerId(currentPhaseVo, runnerMapVo.getRunnerMapId(), jobVo, null, null);
            }
            autoexecJobService.updateJobNodeStatus(runnerVos, jobVo, JobNodeStatus.IGNORED.getValue());
        }

        return null;
    }
}
