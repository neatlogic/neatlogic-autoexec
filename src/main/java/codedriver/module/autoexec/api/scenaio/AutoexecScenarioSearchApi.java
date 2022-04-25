package codedriver.module.autoexec.api.scenario;

import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.restful.annotation.Description;

import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/4/15 3:04 下午
 */
@Service

@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScenarioSearchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecScenarioMapper autoexecScenarioMapper;

    @Override
    public String getName() {
        return "查询场景列表";
    }

    @Override
    public String getToken() {
        return "autoexec/scenario/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Description(desc = "查询场景列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScenarioVo paramScenarioVo = paramObj.toJavaObject(AutoexecScenarioVo.class);
        List<AutoexecScenarioVo> returnScenarioList = new ArrayList<>();
        int ScenarioCount = autoexecScenarioMapper.getScenarioCount(paramScenarioVo);
        if (ScenarioCount > 0) {
            paramScenarioVo.setRowNum(ScenarioCount);
            returnScenarioList = autoexecScenarioMapper.searchScenario(paramScenarioVo);
            Map<Object, Integer> ciEntityReferredCountMap = DependencyManager.getBatchDependencyCount(AutoexecFromType.AUTOEXEC_SCENARIO_CIENTITY, returnScenarioList.stream().map(AutoexecScenarioVo::getId).collect(Collectors.toList()));
            if (!ciEntityReferredCountMap.isEmpty()) {
                for (AutoexecScenarioVo scenarioVo : returnScenarioList) {
                    scenarioVo.setCiEntityReferredCount(ciEntityReferredCountMap.get(scenarioVo.getId()));
                }
            }
        }
        return TableResultUtil.getResult(returnScenarioList, paramScenarioVo);
    }
}
