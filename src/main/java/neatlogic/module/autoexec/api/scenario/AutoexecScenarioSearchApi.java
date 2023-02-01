package neatlogic.module.autoexec.api.scenario;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
@AuthAction(action = AUTOEXEC_BASE.class)
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
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Description(desc = "查询场景列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScenarioVo paramScenarioVo = new AutoexecScenarioVo();
        List<AutoexecScenarioVo> returnScenarioList = new ArrayList<>();
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            paramScenarioVo.setDefaultValue(defaultValue);
            returnScenarioList = autoexecScenarioMapper.searchScenario(paramScenarioVo);
        } else {
            paramScenarioVo = paramObj.toJavaObject(AutoexecScenarioVo.class);
            int ScenarioCount = autoexecScenarioMapper.getScenarioCount(paramScenarioVo);
            if (ScenarioCount > 0) {
                paramScenarioVo.setRowNum(ScenarioCount);
                returnScenarioList = autoexecScenarioMapper.searchScenario(paramScenarioVo);
                // 补充场景被组合工具引用的个数
                Map<Object, Integer> dependencyCountMap = DependencyManager.getBatchDependencyCount(AutoexecFromType.SCENARIO, returnScenarioList.stream().map(AutoexecScenarioVo::getId).collect(Collectors.toList()));
                if (MapUtils.isNotEmpty(dependencyCountMap)) {
                    for (AutoexecScenarioVo scenarioVo : returnScenarioList) {
                        scenarioVo.setReferredCount(dependencyCountMap.get(scenarioVo.getId().toString()));
                    }
                }
            }
        }
        return TableResultUtil.getResult(returnScenarioList, paramScenarioVo);
    }
}
