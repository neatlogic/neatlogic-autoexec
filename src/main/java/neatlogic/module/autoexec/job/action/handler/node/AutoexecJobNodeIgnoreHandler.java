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
import neatlogic.framework.common.util.PageUtil;
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
        Integer isAll = jobVo.getActionParam().getInteger("isAll");
        AutoexecJobPhaseVo currentPhaseVo = jobVo.getExecutePhase();
        IAutoexecJobSourceTypeHandler handler = null;
        if (Objects.equals(currentPhaseVo.getExecMode(), ExecMode.SQL.getValue())) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            handler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            handler.ignoreSql(jobVo.getActionParam(), jobVo);
        } else {
            if (Objects.equals(isAll, 1)) {
                Integer count = autoexecJobMapper.getAutoexecJobNodeCountByJobPhaseIdAndExcludeStatusList(currentPhaseVo.getId(), List.of(JobNodeStatus.RUNNING.getValue(), JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue()));
                if (count == 0) {
                    return null;
                }
                int pageSize = 1000;
                int rowNum = PageUtil.getPageCount(count, pageSize);
                for (int i = 0; i < rowNum; i++) {
                    List<AutoexecJobPhaseNodeVo> nodes = autoexecJobMapper.getAutoexecJobNodeListByJobPhaseIdAndExcludeStatusList(currentPhaseVo.getId(),
                            List.of(JobNodeStatus.RUNNING.getValue(), JobNodeStatus.IGNORED.getValue(), JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.INVALID.getValue()), i, pageSize);
                    jobVo.setExecuteJobNodeVoList(nodes);
                    ignoreNodes(jobVo);
                }
            } else {
                currentResourceIdListValid(jobVo);
                ignoreNodes(jobVo);
            }
        }
        return null;
    }

    /**
     * 忽略一批节点
     *
     * @param jobVo 作业入参
     */
    private void ignoreNodes(AutoexecJobVo jobVo) {
        List<RunnerMapVo> runnerVos = new ArrayList<>();
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeRunnerListByNodeIdList(jobVo.getExecuteJobNodeVoList().stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getExecuteJobNodeVoList()) {
            nodeVo.setStatus(JobNodeStatus.IGNORED.getValue());
            autoexecJobMapper.updateJobPhaseNodeById(nodeVo);
        }
        for (AutoexecJobPhaseNodeVo nodeVo : nodeVoList) {
            runnerVos.add(new RunnerMapVo(nodeVo.getRunnerUrl(), nodeVo.getRunnerMapId()));
        }
        runnerVos = runnerVos.stream().filter(o -> StringUtils.isNotBlank(o.getUrl())).collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(RunnerMapVo::getUrl))), ArrayList::new));

        for (RunnerMapVo runnerMapVo : runnerVos) {
            autoexecJobService.updatePartialNodeJobAndPhaseWithRunnerId(jobVo.getExecutePhase(), runnerMapVo.getRunnerMapId(), jobVo, null, null);
        }
        autoexecJobService.updateJobNodeStatus(runnerVos, jobVo, JobNodeStatus.IGNORED.getValue());
    }
}
