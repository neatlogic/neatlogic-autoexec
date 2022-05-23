/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.operation;

import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecScriptService;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
//@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptAndToolSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecService autoexecService;

    @Resource
    private AutoexecScriptService autoexecScriptService;

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
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "catalogId", type = ApiParamType.LONG, desc = "工具目录ID"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的工具或脚本ID列表"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "工具/脚本列表"),
            @Param(explode = AutoexecOperationVo.class),
    })
    @Description(desc = "查询工具和脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        List<AutoexecOperationVo> tbodyList = new ArrayList<>();
        result.put("tbodyList", tbodyList);
        AutoexecOperationVo searchVo = JSON.toJavaObject(jsonObj, AutoexecOperationVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<AutoexecOperationVo> toolAndScriptList = new ArrayList<>();
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<AutoexecOperationVo> toolList = autoexecToolMapper.getToolListByIdList(idList);
            List<AutoexecOperationVo> scriptList = autoexecScriptMapper.getScriptListByIdList(idList);
            List<AutoexecOperationVo> argumentList = autoexecScriptMapper.getArgumentListByScriptIdList(idList);
            if (scriptList.size() > 0 && argumentList.size() > 0) {
                Map<Long, AutoexecParamVo> argumentMap = argumentList.stream().collect(Collectors.toMap(AutoexecOperationBaseVo::getId, AutoexecOperationVo::getArgument));
                for (AutoexecOperationVo vo : scriptList) {
                    vo.setArgument(argumentMap.get(vo.getId()));
                }
            }
            toolAndScriptList.addAll(toolList);
            toolAndScriptList.addAll(scriptList);
            for (AutoexecOperationVo autoexecToolAndScriptVo : toolAndScriptList) {
                List<AutoexecParamVo> paramList = autoexecToolAndScriptVo.getParamList();
                if (CollectionUtils.isNotEmpty(paramList)) {
                    for (AutoexecParamVo autoexecParamVo : paramList) {
                        autoexecService.mergeConfig(autoexecParamVo);
                    }
                }
            }
            // 按传入的valueList排序
            if (CollectionUtils.isNotEmpty(toolAndScriptList)) {
                for (Long id : idList) {
                    Optional<AutoexecOperationVo> first = toolAndScriptList.stream().filter(o -> Objects.equals(o.getId(), id)).findFirst();
                    first.ifPresent(tbodyList::add);
                }
            }
            return result;
        }
        //查询各级子目录
        searchVo.setCatalogIdList(autoexecScriptService.getCatalogIdList(searchVo.getCatalogId()));
        // execMode为native的工具可以被任意阶段引用，不受阶段的execMode限制
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
