/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerFactory;
import codedriver.framework.autoexec.job.source.action.IAutoexecJobSourceActionHandler;
import codedriver.framework.deploy.constvalue.DeployOperType;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeSqlContentGetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeSqlContentGetHandler.class);

    @Override
    public String getName() {
        return JobAction.GET_NODE_SQL_CONTENT.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        AutoexecJobVo jonInfo = autoexecJobMapper.getJobInfo(jobVo.getCurrentNode().getJobId());
        if (jonInfo != null) {
            if (StringUtils.equals(jonInfo.getSource(), DeployOperType.DEPLOY.getValue())) {
                IAutoexecJobSourceActionHandler jobSourceActionHandler = AutoexecJobSourceActionHandlerFactory.getAction(DeployOperType.DEPLOY.getValue());
                result.put("content", jobSourceActionHandler.getJobSqlContent(jobVo));
            } else {
                IAutoexecJobSourceActionHandler jobSourceActionHandler = AutoexecJobSourceActionHandlerFactory.getAction(AutoexecOperType.AUTOEXEC.getValue());
                result.put("content", jobSourceActionHandler.getJobSqlContent(jobVo));
            }
        }
        return result;
    }
}
