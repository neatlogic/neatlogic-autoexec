package neatlogic.module.autoexec.dependency;

import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.dependency.core.CustomDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 自动化工具目录引用自定义工具处理器
 *
 * @author longrf
 * @date 2021/12/15 3:59 下午
 */
@Service
public class AutoexecCatalogDependencyHandler extends CustomDependencyHandlerBase {

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
        return "autoexec_script";
    }

    /**
     * 被引用者（上游）字段
     *
     * @return
     */
    @Override
    protected String getFromField() {
        return "catalog_id";
    }

    /**
     * 引用者（下游）字段
     *
     * @return
     */
    @Override
    protected String getToField() {
        return "id";
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
            return  autoexecScriptService.getScriptDependencyPageUrl(map, (Long) map.get("id"),this.getGroupName());
        }
        return null;
    }

    /**
     * 被引用者（上游）类型
     *
     * @return
     */
    @Override
    public IFromType getFromType() {
        return AutoexecFromType.AUTOEXEC_CATALOG;
    }
}
