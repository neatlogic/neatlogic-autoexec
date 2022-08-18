/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.AutoexecJobSourceVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.source.AutoexecJobSourceFactory;
import codedriver.framework.exception.type.ParamIrregularException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
            AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
            if (jobSourceVo == null) {
                throw new AutoexecJobSourceInvalidException(jobVo.getSource());
            }
            nodeVoList = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType()).getJobNodeListBySqlIdList(sqlIdArray.toJavaList(Long.class));
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
        executeNode(jobVo);
        return null;
    }
}
