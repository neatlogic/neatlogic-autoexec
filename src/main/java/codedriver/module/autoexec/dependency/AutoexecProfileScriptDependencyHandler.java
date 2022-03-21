package codedriver.module.autoexec.dependency;

import codedriver.framework.autoexec.constvalue.FromType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.module.autoexec.service.AutoexecScriptService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 工具profile引用自定义工具处理器
 *
 * @author longrf
 * @date 2022/3/18 10:36 上午
 */
@Service
public class AutoexecProfileScriptDependencyHandler extends CustomTableDependencyHandlerBase {

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    AutoexecScriptService autoexecScriptService;

    /**
     * 表名
     *
     * @return
     */
    @Override
    protected String getTableName() {
        return "autoexec_profile_script";
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
        return "script_id";
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
            return autoexecScriptService.getScriptDependencyPageUrl(map, this.getGroupName());
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FromType.AUTOEXEC_PROFILE_TOOL_AND_SCRIPT;
    }
}
