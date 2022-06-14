package codedriver.module.autoexec.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
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
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecProfileSearchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Override
    public String getName() {
        return "查询自动化工具profile列表";
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
            @Param(name = "operationId", desc = "工具id", type = ApiParamType.LONG),
            @Param(name = "type", desc = "工具类型", type = ApiParamType.STRING),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "关联配置项id"),
            @Param(name = "fromSystemId", type = ApiParamType.LONG, desc = "所属系统id"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "keyword", desc = "关键词（名称、描述）", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "needPage", desc = "是否分页", type = ApiParamType.BOOLEAN),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = AutoexecProfileVo[].class, desc = "工具profile列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "自动化profile列表查询接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecProfileVo paramProfileVo = JSON.toJavaObject(paramObj, AutoexecProfileVo.class);
        List<AutoexecProfileVo> returnList = new ArrayList<>();
        int profileCount = autoexecProfileMapper.searchAutoexecProfileCount(paramProfileVo);
        if (profileCount > 0) {
            paramProfileVo.setRowNum(profileCount);
            returnList = autoexecProfileMapper.searchAutoexecProfile(paramProfileVo);
            if (CollectionUtils.isEmpty(returnList)) {
                returnList = new ArrayList<>();
            }
        }
        return TableResultUtil.getResult(returnList, paramProfileVo);
    }
}
