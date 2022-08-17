/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/8/17 12:18
 **/
@Service
public class AutoexecJobTakeOverHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return JobAction.TAKE_OVER.getValue();
    }

    @Override
    public boolean isNeedExecuteAuthCheck(){
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        jobVo.setExecUser(UserContext.get().getUserUuid());
        autoexecJobMapper.updateJobExecUser(jobVo.getId(), jobVo.getExecUser());
        return null;
    }
}
