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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeReFireHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return JobAction.REFIRE_NODE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        JSONObject jsonObj = jobVo.getActionParam();
        List<Long> resourceIdList = JSONObject.parseArray(jsonObj.getJSONArray("resourceIdList").toJSONString(), Long.class);
        if(CollectionUtils.isEmpty(resourceIdList)){
            throw new ParamIrregularException("resourceIdList");
        }
        List<AutoexecJobPhaseNodeVo> nodeVoList;
        if (Objects.equals(jobVo.getCurrentPhase().getExecMode(), ExecMode.SQL.getValue())) {
            JSONArray sqlIdArray = jobVo.getActionParam().getJSONArray("sqlIdList");
            if(CollectionUtils.isEmpty(sqlIdArray)){
                throw new ParamIrregularException("sqlIdList");
            }
            IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
            if (jobSource == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            nodeVoList = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType()).getJobNodeListBySqlIdList(sqlIdArray.toJavaList(Long.class));
            jobVo.setJobPhaseNodeSqlList(nodeVoList);
        }else {
            nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdList(jobVo.getCurrentPhaseId(), resourceIdList);
            //重置节点开始和结束时间,以防 失败节点直接"重跑"导致耗时异常
            autoexecJobMapper.updateJobPhaseNodeResetStartTimeAndEndTimeByNodeIdList(nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        }
        jobVo.setExecuteJobNodeVoList(nodeVoList);
        //校验是否和当前phaseId一致
       /* for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : nodeVoList) {
            if (!Objects.equals(jobPhaseNodeVo.getJobPhaseId(), jobVo.getCurrentPhaseId())) {
                throw new ParamIrregularException("resourceIdList");
            }
        }*/
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        //重跑单个节点无需激活下个phase
        jobVo.setIsNoFireNext(1);
        //跟新phase状态为running
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        phaseVo.setStatus(JobPhaseStatus.RUNNING.getValue());
        autoexecJobMapper.updateJobPhaseStatus(phaseVo);
        jobVo.setExecuteJobPhaseList(Collections.singletonList(phaseVo));
        autoexecJobService.executeNode(jobVo);
        return null;
    }
}
