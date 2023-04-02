/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.job;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeStatusCountVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Deprecated
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobDetailGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return "获取作业详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "当作业状态"),
    })
    @Output({
            @Param(explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(name = "isRefresh", type = ApiParamType.INTEGER, isRequired = true, desc = "是否需要继续定时刷新，1:继续 0:停止")
    })
    @Description(desc = "获取作业详情，包括：剧本列表、作业基本信息、操作按钮")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //剧本列表
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobId);
        List<AutoexecJobPhaseNodeStatusCountVo> statusCountVoList = autoexecJobMapper.getJobPhaseNodeStatusCount(jobId);
        for (AutoexecJobPhaseNodeStatusCountVo statusCountVo : statusCountVoList) {
            for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
                if (statusCountVo.getJobPhaseId().equals(phaseVo.getId())) {
                    phaseVo.addStatusCountVo(statusCountVo);
                }
            }
        }
        jobVo.setPhaseList(jobPhaseVoList);
        //判断是否有执行与接管权限
        if(!Objects.equals(jobVo.getStatus(), JobStatus.CHECKED.getValue())) {
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
            autoexecJobSourceActionHandler.getJobActionAuth(jobVo);
        }

        return jobVo;
    }

    @Override
    public String getToken() {
        return "autoexec/job/detail/get";
    }
}
