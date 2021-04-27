/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptAndToolSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/scriptandtool/search";
    }

    @Override
    public String getName() {
        return "查询工具和脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "type", type = ApiParamType.ENUM, rule = "tool,script", desc = "类别(工具；脚本)"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "valueList", type = ApiParamType.JSONARRAY, desc = "用于回显的工具或脚本ID列表"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "工具/脚本列表"),
            @Param(explode = AutoexecToolAndScriptVo.class),
    })
    @Description(desc = "查询工具和脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        List<AutoexecToolAndScriptVo> tbodyList = new ArrayList<>();
        result.put("tbodyList", tbodyList);
        AutoexecToolAndScriptVo searchVo = JSON.toJavaObject(jsonObj, AutoexecToolAndScriptVo.class);
        JSONArray valueList = jsonObj.getJSONArray("valueList");
        if (CollectionUtils.isNotEmpty(valueList)) {
            List<AutoexecToolAndScriptVo> toolAndScriptList = new ArrayList<>();
            List<Long> idList = valueList.toJavaList(Long.class);
            List<AutoexecToolAndScriptVo> toolList = autoexecToolMapper.getToolListByIdList(idList);
            List<AutoexecToolAndScriptVo> scriptList = autoexecScriptMapper.getScriptListByIdList(idList);
            toolAndScriptList.addAll(toolList);
            toolAndScriptList.addAll(scriptList);
            // 按传入的valueList排序
            if (CollectionUtils.isNotEmpty(toolAndScriptList)) {
                for (Long id : idList) {
                    Optional<AutoexecToolAndScriptVo> first = toolAndScriptList.stream().filter(o -> Objects.equals(o.getId(), id)).findFirst();
                    if (first != null && first.isPresent()) {
                        tbodyList.add(first.get());
                    }
                }
            }
            return result;
        }
        tbodyList.addAll(autoexecScriptMapper.searchScriptAndTool(searchVo));
        if (searchVo.getNeedPage()) {
            int rowNum = autoexecScriptMapper.searchScriptAndToolCount(searchVo);
            searchVo.setRowNum(rowNum);
            result.put("currentPage", searchVo.getCurrentPage());
            result.put("pageSize", searchVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, searchVo.getPageSize()));
            result.put("rowNum", searchVo.getRowNum());
        }

        return result;
    }


}
