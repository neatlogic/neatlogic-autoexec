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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TimeUtil;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/13 10:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobCombopSearchApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Override
    public String getName() {
        return "nmaa.autoexecjobcombopsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.jobstatuslist"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.jobsourcelist"),
            @Param(name = "combopOperationTypeList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecjobcombopsearchapi.input.param.desc.combopoperationtypelist"),
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "term.autoexec.combopname"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.starttimefilter"),
            @Param(name = "execUser", type = ApiParamType.STRING, desc = "nmaa.autoexecjobcombopsearchapi.input.param.desc.execuser"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmaa.common.input.param.desc.needpage")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "nmaa.common.output.param.desc.datalist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmaa.autoexecjobcombopsearchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        JSONObject startTimeJson = jsonObj.getJSONObject("startTime");
        if (MapUtils.isNotEmpty(startTimeJson)) {
            JSONObject timeJson = TimeUtil.getStartTimeAndEndTimeByDateJson(startTimeJson);
            jsonObj.put("startTime",timeJson.getDate("startTime"));
            jsonObj.put("endTime",timeJson.getDate("endTime"));
        }
        jsonObj.put("operationId",jsonObj.getLong("combopId"));
        jsonObj.put("invokeId",jsonObj.getLong("scheduleId"));
        AutoexecJobVo jobVo = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        List<AutoexecCombopVo> combopVoList = autoexecJobMapper.searchJobWithCombopView(jobVo);
        result.put("tbodyList", combopVoList);
        if (jobVo.getNeedPage()) {
            int rowNum = autoexecJobMapper.searchJobCount(jobVo);
            jobVo.setRowNum(rowNum);
            result.put("currentPage", jobVo.getCurrentPage());
            result.put("pageSize", jobVo.getPageSize());
            result.put("pageCount", jobVo.getPageCount());
            result.put("rowNum", jobVo.getRowNum());
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/combop/search";
    }
}
