package neatlogic.module.autoexec.api.profile;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/16 11:23 上午
 */

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecProfileSearchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Override
    public String getName() {
        return "nmaa.autoexecprofilesearchapi.getname";
    }

    @Override
    public String getToken() {
        return "autoexec/profile/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", desc = "term.autoexec.operationid", type = ApiParamType.LONG),
            @Param(name = "type", desc = "term.autoexec.operationtype", type = ApiParamType.STRING),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "term.cmdb.cientityid"),
            @Param(name = "fromSystemId", type = ApiParamType.LONG, desc = "term.autoexec.sourcesystemid"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "term.autoexec.defaultvalue"),
            @Param(name = "keyword", desc = "nmaa.autoexecprofilesearchapi.input.param.desc.keyword", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "common.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "needPage", desc = "common.needpage", type = ApiParamType.BOOLEAN),
            @Param(name = "pageSize", desc = "common.pagesize", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = AutoexecProfileVo[].class, desc = "nmaa.autoexecprofilesearchapi.output.param.desc.tbodylist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmaa.autoexecprofilesearchapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecProfileVo paramProfileVo = JSON.toJavaObject(paramObj, AutoexecProfileVo.class);
        List<AutoexecProfileVo> returnList = new ArrayList<>();
        int profileCount = autoexecProfileMapper.searchAutoexecProfileCount(paramProfileVo);
        if (profileCount > 0) {
            paramProfileVo.setRowNum(profileCount);
            returnList = autoexecProfileMapper.searchAutoexecProfile(paramProfileVo);
            // 补充关联对象个数
            Map<Object, Integer> dependencyCountMap = DependencyManager.getBatchDependencyCount(AutoexecFromType.PROFILE, returnList.stream().map(AutoexecProfileVo::getId).collect(Collectors.toList()));
            for (AutoexecProfileVo profileVo : returnList) {
                if (dependencyCountMap.containsKey(profileVo.getId().toString())) {
                    profileVo.setReferredCount(dependencyCountMap.get(profileVo.getId().toString()));
                }
            }
            if (CollectionUtils.isEmpty(returnList)) {
                returnList = new ArrayList<>();
            }
        }
        return TableResultUtil.getResult(returnList, paramProfileVo);
    }
}
