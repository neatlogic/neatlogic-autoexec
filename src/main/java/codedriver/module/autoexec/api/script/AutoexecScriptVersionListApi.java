/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/version/list";
    }

    @Override
    public String getName() {
        return "获取脚本版本列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scriptId", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
    })
    @Output({
            @Param(name = "currentVersion", explode = AutoexecScriptVersionVo.class, desc = "当前版本"),
            @Param(name = "notPassedVersionList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVersionVo[].class, desc = "未审批通过版本列表"),
            @Param(name = "historicalVersionList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVersionVo[].class, desc = "历史版本列表"),
    })
    @Description(desc = "获取脚本版本列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long scriptId = jsonObj.getLong("scriptId");
        if (autoexecScriptMapper.checkScriptIsExistsById(scriptId) == 0) {
            throw new AutoexecScriptNotFoundException(scriptId);
        }
        // 当前激活版本
        AutoexecScriptVersionVo currentVersion = null;
        // 非审核通过版本
        List<AutoexecScriptVersionVo> notPassedVersionList = autoexecScriptMapper.getNotPassedVersionListByScriptId(scriptId);
        // 历史审核通过版本
        List<AutoexecScriptVersionVo> historicalVersionList = autoexecScriptMapper.getHistoricalVersionListByScriptId(scriptId);
        if (CollectionUtils.isNotEmpty(historicalVersionList)) {
            Optional<AutoexecScriptVersionVo> first = historicalVersionList.stream().filter(o -> Objects.equals(o.getIsActive(), 1)).findFirst();
            if (first != null && first.isPresent()) {
                currentVersion = first.get();
            }
            historicalVersionList = historicalVersionList.stream().filter(o -> !Objects.equals(o.getIsActive(), 1)).collect(Collectors.toList());
        }
        result.put("currentVersion", currentVersion);
        result.put("notPassedVersionList", notPassedVersionList);
        result.put("historicalVersionList", historicalVersionList);
        return result;
    }
}
