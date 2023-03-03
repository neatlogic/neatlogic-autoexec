package neatlogic.module.autoexec.dependency;

import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.dependency.core.CustomTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/29 2:28 下午
 * 预制参数集引用自定义工具处理器
 */
@Service
public class AutoexecScript2ProfileDependencyHandler extends CustomTableDependencyHandlerBase {
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
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCRIPT;
    }
}
