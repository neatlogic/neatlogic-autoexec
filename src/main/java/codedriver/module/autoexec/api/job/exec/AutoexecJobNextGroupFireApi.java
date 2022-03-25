/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobGroupVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.exception.type.ParamInvalidException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobNextGroupFireApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "激活作业下一组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "groupNo", type = ApiParamType.STRING, desc = "下一个组（排序）序号", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数", isRequired = true),
            @Param(name = "time", type = ApiParamType.DOUBLE, desc = "回调时间")
    })
    @Output({
    })
    @Description(desc = "激活作业下一组")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Integer groupSort = jsonObj.getInteger("groupNo");
        Long runnerId;
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");

        if (MapUtils.isEmpty(passThroughEnv)) {
            throw new ParamIrregularException("passThroughEnv");
        }
        if (!passThroughEnv.containsKey("runnerId")) {
            throw new ParamIrregularException("passThroughEnv:runnerId");
        } else {
            runnerId = passThroughEnv.getLong("runnerId");
        }

        if (!passThroughEnv.containsKey("groupSort")) {
            throw new ParamIrregularException("passThroughEnv:groupSort");
        } else {
            if (!Objects.equals(passThroughEnv.getInteger("groupSort"), groupSort)) {
                throw new ParamInvalidException("groupSort", groupSort.toString());
            }
        }
        //更新group对应runner的"是否fireNext"标识为1
        autoexecJobMapper.updateJobPhaseRunnerFireNextByJobIdAndGroupSortAndRunnerId(jobId, groupSort, 1, runnerId);
        /*
         *判断是否满足激活下个phase条件
         * 1、当前sort的所有phase都completed
         * 2、当前sort的所有phase的runner 都是completed，所有runner的"是否fireNext"标识都为1
         */
        if (autoexecJobService.checkIsAllActivePhaseIsCompleted(jobId, groupSort)) {
            AutoexecJobGroupVo nextGroupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobId, groupSort + 1);
            if (nextGroupVo != null) {
                jobVo.setExecuteJobGroupVo(nextGroupVo);
                IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                fireAction.doService(jobVo);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/next/group/fire";
    }
}
