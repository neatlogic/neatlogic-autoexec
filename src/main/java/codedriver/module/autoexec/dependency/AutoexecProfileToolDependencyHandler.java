package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 工具profile引用工具库工具处理器
 *
 * @author longrf
 * @date 2022/3/18 10:36 上午
 */
@Service
public class AutoexecProfileToolDependencyHandler extends CustomTableDependencyHandlerBase {

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    /**
     * 表名
     *
     * @return
     */
    @Override
    protected String getTableName() {
        return "autoexec_profile_tool";
    }

    /**
     * 被引用者（上游）字段
     *
     * @return
     */
    @Override
    protected String getFromField() {
        return "profile_id";
    }

    /**
     * 引用者（下游）字段
     *
     * @return
     */
    @Override
    protected String getToField() {
        return "tool_id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    /**
     * 解析数据，拼装跳转url，返回引用下拉列表一个选项数据结构
     *
     * @param dependencyObj 调用者值
     * @return
     */
    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj instanceof Map) {
            Map<String, Object> map = (Map) dependencyObj;
            Long toolId = (Long) map.get("tool_id");
            AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(toolId);
            if (autoexecToolVo != null) {
                JSONObject dependencyInfoConfig = new JSONObject();
                dependencyInfoConfig.put("toolId", autoexecToolVo.getId());
                dependencyInfoConfig.put("toolName", autoexecToolVo.getName());
                String pathFormat = AutoexecFromType.AUTOEXEC_PROFILE_TOOL_AND_SCRIPT.getText() + "-${DATA.toolName}";
                String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/tool-detail?id=#{DATA.toolId}";
                return new DependencyInfoVo(autoexecToolVo.getId(), dependencyInfoConfig, pathFormat, urlFormat, this.getGroupName());
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.AUTOEXEC_PROFILE_TOOL_AND_SCRIPT;
    }
}
