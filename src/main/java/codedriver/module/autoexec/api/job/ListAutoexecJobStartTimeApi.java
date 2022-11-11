/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecJobStartTimeApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "autoexec/job/starttime/list";
    }

    @Override
    public String getName() {
        return "作业开始时间列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "作业来源"),
            @Param(name = "invokeId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具定时作业Id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
    })
    @Output({
            @Param(name = "tbodyList", explode = ValueTextVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "作业开始时间列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = JSONObject.toJavaObject(paramObj, AutoexecJobVo.class);
        int rowNum = autoexecJobMapper.getAutoexecJobStartTimeCount(jobVo);
        if (rowNum > 0) {
            jobVo.setRowNum(rowNum);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            List<ValueTextVo> tbodyList = new ArrayList<>();
            List<String> startTimeList = autoexecJobMapper.getAutoexecJobStartTimeList(jobVo);
            for (String startTime : startTimeList) {
                Date date = sdf.parse(startTime);
                tbodyList.add(new ValueTextVo(date.getTime(), startTime));
            }
            return TableResultUtil.getResult(tbodyList, jobVo);
        }
        return TableResultUtil.getResult(new ArrayList<>(), jobVo);
    }
}
