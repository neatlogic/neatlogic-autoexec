/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionListForSelectApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/version/list/forselect";
    }

    @Override
    public String getName() {
        return "获取脚本版本号列表(下拉)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scriptId", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词"),
            @Param(name = "excludeList", type = ApiParamType.JSONARRAY, desc = "需要排除的版本ID列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVersionVo[].class, desc = "版本号列表"),
    })
    @Description(desc = "获取脚本版本号列表(下拉)")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVersionVo vo = jsonObj.toJavaObject(AutoexecScriptVersionVo.class);
        if (autoexecScriptMapper.checkScriptIsExistsById(vo.getScriptId()) == 0) {
            throw new AutoexecScriptNotFoundException(vo.getScriptId());
        }
        if (jsonObj.getInteger("pageSize") == null) {
            vo.setPageSize(100);
        }
        if (!AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            vo.setStatus(ScriptVersionStatus.PASSED.getValue());
        }
        int rowNum = autoexecScriptMapper.searchVersionCountForSelect(vo);
        List<AutoexecScriptVersionVo> list = autoexecScriptMapper.searchVersionListForSelect(vo);
        vo.setRowNum(rowNum);
        result.put("currentPage", vo.getCurrentPage());
        result.put("pageSize", vo.getPageSize());
        result.put("pageCount", PageUtil.getPageCount(rowNum, vo.getPageSize()));
        result.put("rowNum", vo.getRowNum());
        result.put("tbodyList", list);
        return result;
    }

}
