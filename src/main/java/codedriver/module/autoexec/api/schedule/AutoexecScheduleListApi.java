package codedriver.module.autoexec.api.schedule;

import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
//@AuthAction(action = SCHEDULE_JOB_MODIFY.class)

@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScheduleListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/schedule/list";
    }

    @Override
    public String getName() {
        return "查询定时作业列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "定时作业名称(支持模糊查询)"),
            @Param(name = "autoexecCombopId", type = ApiParamType.LONG, desc = "组合工具id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecScheduleVo[].class, desc = "定时作业列表"),
    })
    @Description(desc = "查询定时作业列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScheduleVo autoexecScheduleVo = JSONObject.toJavaObject(paramObj, AutoexecScheduleVo.class);
        Long autoexecCombopId = autoexecScheduleVo.getAutoexecCombopId();
        if (autoexecCombopId != null) {
            if (autoexecCombopMapper.checkAutoexecCombopIsExists(autoexecCombopId) == 0) {
                throw new AutoexecCombopNotFoundException(autoexecCombopId);
            }
        }

        int rowNum = autoexecScheduleMapper.getAutoexecScheduleCount(autoexecScheduleVo);
        autoexecScheduleVo.setRowNum(rowNum);
        List<AutoexecScheduleVo> autoexecScheduleList = autoexecScheduleMapper.getAutoexecScheduleList(autoexecScheduleVo);
        return TableResultUtil.getResult(autoexecScheduleList, autoexecScheduleVo);
    }

}
