package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/10 11:11 上午
 */
@Service
public class AutoexecProfileGlobalParamDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        JSONObject dependencyInfoConfig = new JSONObject();
        if (MapUtils.isNotEmpty(config)) {
            Long profileId = dependencyVo.getConfig().getLong("profileId");
            AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(profileId);
            if (profileVo == null) {
                return null;
            }
            dependencyInfoConfig.put("profileId", profileId);
            List<String> pathList = new ArrayList<>();
            pathList.add("预制参数集");
            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/tool-profile-manage?id=${DATA.profileId}";
            return new DependencyInfoVo(profileVo.getId(), dependencyInfoConfig, profileVo.getName(), pathList, urlFormat, this.getGroupName());

        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.GLOBAL_PARAM;
    }
}
