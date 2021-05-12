/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.operate.ScriptOperateBuilder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/search";
    }

    @Override
    public String getName() {
        return "查询脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "isReviewing", type = ApiParamType.ENUM, rule = "0,1", desc = "是否待审批"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVo[].class, desc = "脚本列表"),
            @Param(name = "reviewingCount", type = ApiParamType.INTEGER, desc = "待审批的脚本数"),
            @Param(name = "operateList", type = ApiParamType.JSONARRAY, desc = "操作按钮"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        List<AutoexecScriptVo> scriptVoList = autoexecScriptMapper.searchScript(scriptVo);
        result.put("tbodyList", scriptVoList);
        if (scriptVo.getNeedPage()) {
            int rowNum = autoexecScriptMapper.searchScriptCount(scriptVo);
            scriptVo.setRowNum(rowNum);
            result.put("currentPage", scriptVo.getCurrentPage());
            result.put("pageSize", scriptVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, scriptVo.getPageSize()));
            result.put("rowNum", scriptVo.getRowNum());
        }
        scriptVo.setIsReviewing(1);
        result.put("reviewingCount", autoexecScriptMapper.searchScriptCount(scriptVo));
        ScriptOperateBuilder builder = new ScriptOperateBuilder(UserContext.get().getUserUuid());
        result.put("operateList", builder.setGenerateToCombop().setCopy().setExport().setDelete().build());
        return result;
    }


}
