/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "组合工具"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具Id"),
            @Param(name = "scheduleId", type = ApiParamType.LONG, desc = "组合工具定时作业Id"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "时间过滤"),
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
        if (MapUtils.isNotEmpty(startTimeJson)) {
            JSONObject timeJson = TimeUtil.getStartTimeAndEndTimeByDateJson(startTimeJson);
            jsonObj.put("startTime", timeJson.getDate("startTime"));
            jsonObj.put("endTime", timeJson.getDate("endTime"));
        }
        jsonObj.put("operationId", jsonObj.getLong("combopId"));
        jsonObj.put("invokeId", jsonObj.getLong("scheduleId"));
        AutoexecJobVo jobVo = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        return TableResultUtil.getResult(autoexecJobService.getJobList(jobVo), jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/search";
    }
}
