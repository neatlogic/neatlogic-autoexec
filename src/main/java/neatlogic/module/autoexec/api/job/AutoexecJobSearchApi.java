/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.api.job;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobSearchApi extends PrivateApiComponentBase {


    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "作业搜索（作业执行列表）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "作业来源"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "组合工具类型"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "作业id列表，用于精确刷新作业状态"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "父作业id"),
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "组合工具"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具Id"),
            @Param(name = "scheduleId", type = ApiParamType.LONG, desc = "组合工具定时作业Id"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "时间过滤"),
            @Param(name = "hasParent", type = ApiParamType.BOOLEAN, desc = "是否拥有父作业"),
            @Param(name = "sortOrder", type = ApiParamType.JSONOBJECT, desc = "排序"),
            @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "操作人"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "作业搜索（作业执行视图）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject startTimeJson = jsonObj.getJSONObject("startTime");
        Long parentId = jsonObj.getLong("parentId");
        if (MapUtils.isNotEmpty(startTimeJson)) {
            JSONObject timeJson = TimeUtil.getStartTimeAndEndTimeByDateJson(startTimeJson);
            jsonObj.put("startTime", timeJson.getDate("startTime"));
            jsonObj.put("endTime", timeJson.getDate("endTime"));
        } else {
            jsonObj.remove("startTime");
        }
        jsonObj.put("operationId", jsonObj.getLong("combopId"));
        jsonObj.put("invokeId", jsonObj.getLong("scheduleId"));
        AutoexecJobVo jobVo = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        if (parentId != null) {
            List<Long> idList = autoexecJobMapper.getJobIdListByParentId(parentId);
            jobVo.setIdList(idList);
            jobVo.setSourceList(new ArrayList<String>() {{
                this.add(JobSource.DEPLOY.getValue());
            }});
        }
        return TableResultUtil.getResult(autoexecJobService.searchJob(jobVo), jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/search";
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }
}
