/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
        JSONObject jsonObj = jobVo.getActionParam();
        List<Long> resourceIdList = JSONObject.parseArray(jsonObj.getJSONArray("resourceIdList").toJSONString(), Long.class);
        //如果是重跑节点，则nodeId 必填
        if(CollectionUtils.isEmpty(resourceIdList)){
            throw new ParamIrregularException("resourceIdList");
        }
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdList(jobVo.getCurrentPhaseId(),resourceIdList);
        List<Long> nodeResourceIdList = nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).collect(Collectors.toList());
        List<Long> notExistResourceIdList = resourceIdList.stream().filter(s -> !nodeResourceIdList.contains(s)).collect(Collectors.toList());
        //无须校验
        /*if(CollectionUtils.isNotEmpty(notExistResourceIdList)){
            throw new AutoexecJobPhaseNodeNotFoundException(StringUtils.EMPTY,notExistResourceIdList.toString());
        }*/
        jobVo.setPhaseNodeVoList(nodeVoList);
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
        List<AutoexecJobPhaseNodeVo> nodeVoList = jobVo.getPhaseNodeVoList();
        //重置节点开始和结束时间,以防 失败节点直接"重跑"导致耗时异常
        autoexecJobMapper.updateJobPhaseNodeResetStartTimeAndEndTimeByNodeIdList(nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList()));
        AutoexecJobPhaseNodeVo nodeVo = nodeVoList.get(0);
        for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : nodeVoList) {
            if (!Objects.equals(jobPhaseNodeVo.getJobPhaseId(), nodeVo.getJobPhaseId())) {
                throw new ParamIrregularException("resourceIdList");
            }
        }
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseId(nodeVo.getJobId(), nodeVo.getJobPhaseId());
        jobVo.setCurrentGroupSort(phaseVo.getSort());
        autoexecJobService.getAutoexecJobDetail(jobVo, phaseVo.getSort());
        //过滤仅需要当前phase的配置
        jobVo.setPhaseList(jobVo.getPhaseList().stream().filter(o -> Objects.equals(phaseVo.getId(), o.getId())).collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(jobVo.getPhaseList())) {
            execute(jobVo);
        }
        return null;
    }
}
