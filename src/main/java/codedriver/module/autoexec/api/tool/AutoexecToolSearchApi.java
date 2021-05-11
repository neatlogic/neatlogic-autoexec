/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecToolSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/search";
    }

    @Override
    public String getName() {
        return "查询工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecToolVo[].class, desc = "工具列表"),
            @Param(name = "operateList", type = ApiParamType.JSONARRAY, desc = "操作按钮"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecToolVo toolVo = JSON.toJavaObject(jsonObj, AutoexecToolVo.class);
        List<AutoexecToolVo> toolVoList = autoexecToolMapper.searchTool(toolVo);
        result.put("tbodyList", toolVoList);
        if (toolVo.getNeedPage()) {
            int rowNum = autoexecToolMapper.searchToolCount(toolVo);
            toolVo.setRowNum(rowNum);
            result.put("currentPage", toolVo.getCurrentPage());
            result.put("pageSize", toolVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, toolVo.getPageSize()));
            result.put("rowNum", toolVo.getRowNum());
        }
        List<ValueTextVo> operateList = new ArrayList<>();
        if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            operateList.add(new ValueTextVo("测试", "test"));
            operateList.add(new ValueTextVo("启用/禁用", "active"));
        }
        result.put("operateList", operateList);
        return result;
    }


}
