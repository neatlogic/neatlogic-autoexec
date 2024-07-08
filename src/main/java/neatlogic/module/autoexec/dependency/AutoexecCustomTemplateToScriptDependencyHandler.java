package neatlogic.module.autoexec.dependency;

import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.dependency.core.CustomDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class AutoexecCustomTemplateToScriptDependencyHandler extends CustomDependencyHandlerBase {

    @Resource
    AutoexecScriptService autoexecScriptService;

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.CUSTOM_TEMPLATE;
    }

    @Override
    protected String getTableName() {
        return "autoexec_script";
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
            return autoexecScriptService.getScriptDependencyPageUrl(map, (Long) map.get("id"), this.getGroupName());
        }
        return null;
    }
}
