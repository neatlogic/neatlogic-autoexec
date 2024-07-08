package neatlogic.module.autoexec.dependency;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.dependency.core.CustomDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AutoexecCustomTemplateToToolDependencyHandler extends CustomDependencyHandlerBase {

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.CUSTOM_TEMPLATE;
    }

    @Override
    protected String getTableName() {
        return "autoexec_tool";
    }

    @Override
    protected String getFromField() {
        return "customtemplate_id";
    }

    @Override
    protected String getToField() {
        return null;
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj == null) {
            return null;
        }
        if (dependencyObj instanceof Map) {
            Map<String, Object> map = (Map) dependencyObj;
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("toolId", map.get("id"));
            List<String> pathList = new ArrayList<>();
            pathList.add("工具库");
            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/tool-detail?id=${DATA.toolId}";
            return new DependencyInfoVo(map.get("id"), dependencyInfoConfig, map.get("name").toString(), pathList, urlFormat, this.getGroupName());
        }
        return null;
    }

}
