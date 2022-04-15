package codedriver.module.autoexec.api.schedule;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScheduleMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import codedriver.framework.autoexec.dto.schedule.AutoexecScheduleVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScheduleListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScheduleMapper autoexecScheduleMapper;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;

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
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用"),
            @Param(name = "autoexecCombopId", type = ApiParamType.LONG, desc = "组合工具id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecScheduleVo[].class, desc = "定时作业列表"),
    })
    @Description(desc = "查询定时作业列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScheduleVo searchVo = JSONObject.toJavaObject(paramObj, AutoexecScheduleVo.class);
        Long autoexecCombopId = searchVo.getAutoexecCombopId();
        if (autoexecCombopId != null) {
            if (autoexecCombopMapper.checkAutoexecCombopIsExists(autoexecCombopId) == 0) {
                throw new AutoexecCombopNotFoundException(autoexecCombopId);
            }
        }
        List<AutoexecScheduleVo> autoexecScheduleList = new ArrayList<>();
        int rowNum = autoexecScheduleMapper.getAutoexecScheduleCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                autoexecScheduleList = autoexecScheduleMapper.getAutoexecScheduleList(searchVo);
                Set<Long> autoexecCombopIdSet = autoexecScheduleList.stream().map(AutoexecScheduleVo::getAutoexecCombopId).collect(Collectors.toSet());
                List<AutoexecCombopVo> autoexecCombopVoList = autoexecCombopMapper.getAutoexecCombopListByIdList(new ArrayList<>(autoexecCombopIdSet));
                Map<Long, AutoexecCombopVo> autoexecCombopMap = autoexecCombopVoList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                List<Long> idList = autoexecScheduleList.stream().map(AutoexecScheduleVo::getId).collect(Collectors.toList());
                List<AutoexecJobInvokeVo> execCountList = autoexecJobMapper.getJobIdCountListByInvokeIdList(idList);
                Map<Long, Integer> execCountMap = execCountList.stream().collect(Collectors.toMap(e -> e.getInvokeId(), e -> e.getCount()));
                for (AutoexecScheduleVo autoexecScheduleVo : autoexecScheduleList) {
                    AutoexecCombopVo autoexecCombopVo = autoexecCombopMap.get(autoexecScheduleVo.getAutoexecCombopId());
                    if (autoexecCombopVo != null) {
                        autoexecScheduleVo.setAutoexecCombopName(autoexecCombopVo.getName());
                        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
                        Integer executable = autoexecCombopVo.getExecutable();
                        autoexecScheduleVo.setDeletable(executable);
                        autoexecScheduleVo.setEditable(executable);
                    }
                    Integer execCount = execCountMap.get(autoexecScheduleVo.getId());
                    if (execCount != null) {
                        autoexecScheduleVo.setExecCount(execCount);
                    }
                }
            }
        }
        return TableResultUtil.getResult(autoexecScheduleList, searchVo);
    }

}
