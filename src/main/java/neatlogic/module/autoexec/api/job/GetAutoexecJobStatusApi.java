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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author lvzk
 * @since 2022/7/18 15:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobStatusApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmaaj.getautoexecjobstatusapi.description.desc";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "term.autoexec.jobid"),
            @Param(name = "jobIdList", type = ApiParamType.JSONARRAY, desc = "nmaaj.getautoexecjobstatusapi.input.param.desc.idlist"),
    })
    @Output({

    })
    @Description(desc = "nmaaj.getautoexecjobstatusapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        JSONArray jobIdArray = jsonObj.getJSONArray("jobIdList");
        if (jobId == null && jobIdArray == null) {
            throw new ParamIrregularException("jobId | jobIdList");
        }
        if (jobId != null) {
            JSONObject result = new JSONObject();
            //作业基本信息
            AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
            if (jobVo == null) {
                throw new AutoexecJobNotFoundException(jobId.toString());
            }
            result.put("status", jobVo.getStatus());
            result.put("statusName", jobVo.getStatusName());
            return result;
        } else {
            Map<String,JSONObject> resultMap = new HashMap<>();
            List<Long> jobIdList = jobIdArray.toJavaList(Long.class);
            List<AutoexecJobVo> jobList = autoexecJobMapper.getJobListByIdList(jobIdList);
            for (AutoexecJobVo autoexecJobVo : jobList) {
                JSONObject result = new JSONObject();
                result.put("status", autoexecJobVo.getStatus());
                result.put("statusName", autoexecJobVo.getStatusName());
                resultMap.put(autoexecJobVo.getId().toString(),result);
            }
            return resultMap;
        }
    }

    @Override
    public String getToken() {
        return "autoexec/job/status/get";
    }
}
