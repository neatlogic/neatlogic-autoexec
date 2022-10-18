/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.AutoexecJobSourceVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobContentVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.autoexec.source.AutoexecJobSourceFactory;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/10/08 10:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobCreatePayloadApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取创建作业payload";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true)
    })
    @Output({
            @Param(explode = AutoexecJobVo.class, desc = "列表"),
    })
    @Description(desc = "获取创建作业payload接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
        autoexecJobSourceHandler.executeAuthCheck(jobVo,false);
        autoexecJobSourceHandler.getCreatePayload(jobVo, result);
        result.put("roundCount", jobVo.getRoundCount());
        //param
        JSONObject param = new JSONObject();
        result.put("param", param);
        result.put("scenarioId",jobVo.getScenarioId());
        AutoexecJobContentVo paramContentVo = autoexecJobMapper.getJobContent(jobVo.getParamHash());
        if(paramContentVo != null && StringUtils.isNotBlank(paramContentVo.getContent())) {
            JSONArray paramContentArray = JSONObject.parseArray(paramContentVo.getContent());
            for (int i = 0; i < paramContentArray.size(); i++) {
                JSONObject paramContent = paramContentArray.getJSONObject(i);
                param.put(paramContent.getString("key"), paramContent.get("value"));
            }
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/create/payload/get";
    }
}
