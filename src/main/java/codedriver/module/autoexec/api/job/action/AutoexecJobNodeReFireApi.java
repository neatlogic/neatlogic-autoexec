/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仅允许phase 和 node 状态都不是running的情况下才能执行重跑动作
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobNodeReFireApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "重跑作业节点";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "nodeIdList", type = ApiParamType.JSONARRAY, desc = "重跑的节点idList",isRequired = true)
    })
    @Output({
    })
    @Description(desc = "重跑作业节点")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        JSONArray nodeIdArray = jsonObj.getJSONArray("nodeIdList");
        List<Long> nodeIdParamList = nodeIdArray.stream().map(o->Long.valueOf(o.toString())).collect(Collectors.toList());
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if(jobVo == null){
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //如果是重跑节点，则nodeId 必填
        if(CollectionUtils.isEmpty(nodeIdParamList)){
            throw new ParamIrregularException("nodeId");
        }
        List<AutoexecJobPhaseNodeVo> nodeVoList = autoexecJobMapper.getJobPhaseNodeListByNodeIdList(nodeIdParamList);
        List<Long> nodeIdList = nodeVoList.stream().map(AutoexecJobPhaseNodeVo::getId).collect(Collectors.toList());
        List<Long> notExistNodeIdList = nodeIdParamList.stream().filter(s -> !nodeIdList.contains(s)).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(notExistNodeIdList)){
            throw new AutoexecJobPhaseNodeNotFoundException(StringUtils.EMPTY,notExistNodeIdList.toString());
        }
        jobVo.setPhaseNodeVoList(nodeVoList);
        autoexecJobActionService.executeAuthCheck(jobVo);
        autoexecJobActionService.refire(jobVo,"refireNode");
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/node/refire";
    }
}
