/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
