/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.job;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.exception.DeployJobCannotExecuteException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
        IAutoexecJobSource jobSource = AutoexecJobSourceFactory.getEnumInstance(jobVo.getSource());
        if (jobSource == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSource.getType());
        try {
            autoexecJobSourceHandler.executeAuthCheck(jobVo, false);
        }catch (DeployJobCannotExecuteException exception){
            throw new PermissionDeniedException(exception.getMessage());
        }
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
