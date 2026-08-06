/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import org.apache.commons.collections4.CollectionUtils;
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
        return "nmaa.autoexecjobsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.jobstatuslist"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.jobsourcelist"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecjobsearchapi.input.param.desc.typeidlist"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecjobsearchapi.input.param.desc.idlist"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "term.autoexec.parentjobid"),
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "term.autoexec.combopname"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "term.autoexec.combopid"),
            @Param(name = "scheduleId", type = ApiParamType.LONG, desc = "nmaa.autoexecjobsearchapi.input.param.desc.scheduleid"),
            @Param(name = "routeId", type = ApiParamType.STRING, desc = "nmaa.autoexecjobsearchapi.input.param.desc.routeid"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.starttimefilter"),
            @Param(name = "hasParent", type = ApiParamType.BOOLEAN, desc = "nmaa.autoexecjobsearchapi.input.param.desc.hasparent"),
            @Param(name = "sortOrder", type = ApiParamType.JSONOBJECT, desc = "common.sort"),
            @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecjobsearchapi.input.param.desc.execuserlist"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "nmaa.common.output.param.desc.datalist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmaa.autoexecjobsearchapi.getname")
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
        AutoexecJobVo jobVo = JSON.toJavaObject(jsonObj, AutoexecJobVo.class);
        if (parentId != null) {
            List<Long> idList = autoexecJobMapper.getJobIdListByParentId(parentId);
            if (CollectionUtils.isEmpty(idList)) {
                return TableResultUtil.getResult(new ArrayList<>(), jobVo);
            }
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
