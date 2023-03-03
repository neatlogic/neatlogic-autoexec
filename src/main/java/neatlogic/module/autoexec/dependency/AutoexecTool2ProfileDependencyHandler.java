package neatlogic.module.autoexec.dependency;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.dependency.core.CustomTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/3/29 2:28 下午
 * 预制参数集引用工具库处理器
 */
@Service
public class AutoexecTool2ProfileDependencyHandler extends CustomTableDependencyHandlerBase {

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Override
    protected String getTableName() {
        return "autoexec_profile_operation";
    }

    @Override
    protected String getFromField() {
        return "operation_id";
    }

    @Override
    protected String getToField() {
        return "profile_id";
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
            Object operationId = map.get("operation_id");
            if (operationId != null) {
                AutoexecToolVo tool = autoexecToolMapper.getToolById(Long.valueOf(operationId.toString()));
                if (tool != null) {
                    JSONObject dependencyInfoConfig = new JSONObject();
                    dependencyInfoConfig.put("toolId", tool.getId());
                    List<String> pathList = new ArrayList<>();
                    pathList.add("工具库");
                    String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/tool-detail?id=${DATA.toolId}";
                    return new DependencyInfoVo(tool.getId(), dependencyInfoConfig, tool.getName(), pathList, urlFormat, this.getGroupName());
                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.TOOL;
    }
}
