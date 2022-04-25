package codedriver.module.autoexec.dependency;

import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/25 2:07 下午
 */
@Service
public class AutoexecProfileCiEntityDependencyHandler extends CustomTableDependencyHandlerBase {

    @Override
    protected String getTableName() {
        return "autoexec_profile_cientity";
    }

    @Override
    protected String getFromField() {
        return "profile_id";
    }

    @Override
    protected String getToField() {
        return "ci_entity_id";
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
        return AutoexecFromType.AUTOEXEC_PROFILE_CIENTITY;
    }
}
