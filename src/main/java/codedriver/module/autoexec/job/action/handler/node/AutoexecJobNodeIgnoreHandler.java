/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeIgnoreHandler extends AutoexecJobActionHandlerBase {
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
        JSONObject jsonObj = jobVo.getActionParam();
        if (CollectionUtils.isEmpty(jsonObj.getJSONArray("resourceIdList"))) {
            throw new ParamIrregularException("resourceIdList");
        }
        List<Long> resourceIdList = JSONObject.parseArray(jsonObj.getJSONArray("resourceIdList").toJSONString(), Long.class);
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByJobPhaseIdAndResourceIdList(jobVo.getCurrentPhaseId(), resourceIdList);
        executeAuthCheck(jobVo);
        jobVo.setAction(JobAction.IGNORE_NODE.getValue());
        if (CollectionUtils.isEmpty(nodeVoList)) {
            throw new AutoexecJobPhaseNodeNotFoundException(StringUtils.EMPTY, resourceIdList.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        jobVo.setPhaseNodeVoList(nodeVoList);
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck() {
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        for (AutoexecJobPhaseNodeVo nodeVo : jobVo.getJobPhaseNodeList()) {
            nodeVo.setStatus(JobNodeStatus.IGNORED.getValue());
            nodeVo.setStartTime(null);
            nodeVo.setEndTime(null);
            autoexecJobMapper.updateJobPhaseNode(nodeVo);
        }
        return null;
    }
}
