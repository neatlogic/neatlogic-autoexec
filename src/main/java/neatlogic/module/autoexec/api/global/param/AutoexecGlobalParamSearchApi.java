package neatlogic.module.autoexec.api.global.param;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/4/19 10:01 上午
 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecGlobalParamSearchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Override
    public String getName() {
        return "查询自动化全局参数列表";
    }

    @Override
    public String getToken() {
        return "autoexec/global/param/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "typeList", type = ApiParamType.JSONARRAY, desc = "类型列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Description(desc = "查询自动化全局参数列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecGlobalParamVo globalParamVo = paramObj.toJavaObject(AutoexecGlobalParamVo.class);
        List<AutoexecGlobalParamVo> globalParamList = new ArrayList<>();
        int paramCount = autoexecGlobalParamMapper.getGlobalParamCount(globalParamVo);
        if (paramCount > 0) {
            globalParamVo.setRowNum(paramCount);
            globalParamList = autoexecGlobalParamMapper.searchGlobalParam(globalParamVo);
            Map<Object, Integer> dependencyCountMap = DependencyManager.getBatchDependencyCount(AutoexecFromType.GLOBAL_PARAM, globalParamList.stream().map(AutoexecGlobalParamVo::getKey).collect(Collectors.toList()));
            for (AutoexecGlobalParamVo paramVo : globalParamList) {
                if (dependencyCountMap.containsKey(paramVo.getKey())) {
                    paramVo.setReferredCount(dependencyCountMap.get(paramVo.getKey()));
                }
            }
        }
        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", globalParamVo.getPageSize());
        returnObj.put("pageCount", globalParamVo.getPageCount());
        returnObj.put("rowNum", globalParamVo.getRowNum());
        returnObj.put("currentPage", globalParamVo.getCurrentPage());
        returnObj.put("tbodyList", globalParamList);
        return returnObj;
    }
}
