/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.dto.job.AutoexecJobEnvVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/9/16 14:15
 **/
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobCreateEnvUpdateApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "跟新作业环境变量（出参）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "作业环境变量名", isRequired = true),
            @Param(name = "envValue", type = ApiParamType.STRING, desc = "作业环境变量值", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "跟新作业环境变量（出参）接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecJobEnvVo jobEnvVo = new AutoexecJobEnvVo(jsonObj.getLong("jobId"), jsonObj.getString("envName"), jsonObj.getString("envValue"));
        autoexecJobMapper.replaceIntoJobEnv(jobEnvVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/env/update";
    }
}
