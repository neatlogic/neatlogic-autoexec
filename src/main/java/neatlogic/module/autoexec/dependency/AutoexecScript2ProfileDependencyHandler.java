package neatlogic.module.autoexec.dependency;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.dependency.core.CustomDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/3/29 2:28 下午
 * 预制参数集引用自定义工具处理器
 */
@Service
public class AutoexecScript2ProfileDependencyHandler extends CustomDependencyHandlerBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

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
            Object profileId = map.get("profile_id");
            if (profileId != null) {
                AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(Long.valueOf(profileId.toString()));
                if (profileVo != null) {
                    JSONObject dependencyInfoConfig = new JSONObject();
                    dependencyInfoConfig.put("profileId", profileVo.getId());
                    List<String> pathList = new ArrayList<>();
                    pathList.add($.t("term.autoexec.profile"));
                    String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/tool-profile-manage?id=${DATA.profileId}";
                    return new DependencyInfoVo(profileVo.getId(), dependencyInfoConfig, profileVo.getName(), pathList, urlFormat, this.getGroupName());
                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCRIPT;
    }
}
