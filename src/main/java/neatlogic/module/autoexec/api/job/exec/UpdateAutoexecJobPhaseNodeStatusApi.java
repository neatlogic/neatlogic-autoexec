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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = AUTOEXEC_JOB_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecJobPhaseNodeStatusApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(UpdateAutoexecJobPhaseNodeStatusApi.class);
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmaa.updateautoexecjobphasenodestatusapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "term.cmdb.resourceid", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "nmaa.updateautoexecjobphasenodestatusapi.input.param.desc.phase", isRequired = true),
            @Param(name = "host", type = ApiParamType.STRING, desc = "nmaa.updateautoexecjobphasenodestatusapi.input.param.desc.host"),
            @Param(name = "port", type = ApiParamType.STRING, desc = "nmaa.updateautoexecjobphasenodestatusapi.input.param.desc.port"),
            @Param(name = "status", type = ApiParamType.STRING, desc = "common.status", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.passthroughenv")
    })
    @Output({
    })
    @Description(desc = "nmaa.updateautoexecjobphasenodestatusapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        //避免并发过大，导致性能问题，以下逻辑没必要控制串行
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        Long resourceId = jsonObj.getLong("resourceId");
        String phaseName = jsonObj.getString("phase");
        AutoexecJobPhaseNodeVo nodeVo = null;
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jsonObj.getLong("jobId"), phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(phaseName);
        }
        //获取node
        if (Arrays.asList(ExecMode.RUNNER.getValue(), ExecMode.SQL.getValue()).contains(jobPhaseVo.getExecMode())) {
            List<AutoexecJobPhaseNodeVo> nodeList = autoexecJobMapper.getJobPhaseNodeListByJobIdAndPhaseId(jobId, jobPhaseVo.getId());
            if (CollectionUtils.isNotEmpty(nodeList)) {
                nodeVo = nodeList.get(0);
            }
        } else {
            nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobIdAndJobPhaseNameAndResourceId(jobId, phaseName, resourceId);
        }
        //不抛异常影响其它节点运行，ignore 就好
        if (nodeVo == null) {
            logger.error("jobId:{} phaseName:{} resourceId:{}",jobId,phaseName,resourceId);
            return null;
        }

        //如果node status 和原本的status 或者  warnCount 一样则无需更新
        if (!Objects.equals(nodeVo.getStatus(), jsonObj.getString("status")) || !Objects.equals(nodeVo.getWarnCount(), jsonObj.getInteger("warnCount"))) {
            nodeVo.setStatus(jsonObj.getString("status"));
            nodeVo.setWarnCount(jsonObj.getInteger("warnCount"));
            //当是sql类型的阶段虚拟节点成功时需判断是否所有sql都执行成功，才能更新虚拟节点状态
            if (Objects.equals(JobNodeStatus.SUCCEED.getValue(), jsonObj.getString("status")) && Objects.equals(ExecMode.SQL.getValue(), jobPhaseVo.getExecMode())) {
                IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
                if (jobSource == null) {
                    throw new AutoexecJobSourceInvalidException(jobVo.getSource());
                }
                IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
                boolean isCanUpdateNodeStatus = autoexecJobSourceActionHandler.getIsCanUpdateSqlNode(jobPhaseVo, nodeVo.getRunnerMapId());
                if (!isCanUpdateNodeStatus) {
                    return null;
                }
            }
            if (Objects.equals(nodeVo.getStatus(), JobNodeStatus.RUNNING.getValue())) {
                nodeVo.setIsExecuted(1);
            }
            autoexecJobMapper.updateJobPhaseNodeStatus(nodeVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/status/update";
    }
}
