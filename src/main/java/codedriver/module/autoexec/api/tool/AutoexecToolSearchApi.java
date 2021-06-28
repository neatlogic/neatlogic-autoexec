/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptAndToolOperate;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.dto.OperateVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
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
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfire", desc = "执行方式"),
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
        if (CollectionUtils.isNotEmpty(toolVoList)) {
            List<Long> idList = toolVoList.stream().map(AutoexecToolVo::getId).collect(Collectors.toList());
            List<AutoexecToolVo> hasBeenGeneratedToCombopList = autoexecToolMapper.checkToolListHasBeenGeneratedToCombop(idList);
            Map<Long, Boolean> hasBeenGeneratedToCombopMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(hasBeenGeneratedToCombopList)) {
                hasBeenGeneratedToCombopList.stream().forEach(o -> hasBeenGeneratedToCombopMap.put(o.getId(), o.getHasBeenGeneratedToCombop() > 0 ? true : false));
            }
            // 获取操作按钮
            Boolean hasScriptModifyAuth = AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName());
            Boolean hasScriptManageAuth = AuthActionChecker.check(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName());
            Boolean hasCombopAddAuth = AuthActionChecker.check(AUTOEXEC_COMBOP_ADD.class.getSimpleName());
            toolVoList.stream().forEach(o -> {
                List<OperateVo> operateList = new ArrayList<>();
                OperateVo test = new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText());
                OperateVo active = new OperateVo(ScriptAndToolOperate.ACTIVE.getValue(), ScriptAndToolOperate.ACTIVE.getText());
                OperateVo generateToCombop = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
                operateList.add(test);
                operateList.add(active);
                operateList.add(generateToCombop);
                if (!hasScriptManageAuth) {
                    active.setDisabled(1);
                    active.setDisabledReason("无权限，请联系管理员");
                }
                if (!hasScriptModifyAuth) {
                    test.setDisabled(1);
                    test.setDisabledReason("无权限，请联系管理员");
                }
                if (hasCombopAddAuth) {
                    if (MapUtils.isNotEmpty(hasBeenGeneratedToCombopMap) && Objects.equals(hasBeenGeneratedToCombopMap.get(o.getId()), true)) {
                        generateToCombop.setDisabled(1);
                        generateToCombop.setDisabledReason("已发布为组合工具");
                    } else if (!Objects.equals(o.getIsActive(), 1)) {
                        generateToCombop.setDisabled(1);
                        generateToCombop.setDisabledReason("当前工具未激活，无法发布为组合工具");
                    }
                } else {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason("无权限，请联系管理员");
                }
                if (CollectionUtils.isNotEmpty(operateList)) {
                    o.setOperateList(operateList);
                }
            });
        }
        if (toolVo.getNeedPage()) {
            int rowNum = autoexecToolMapper.searchToolCount(toolVo);
            toolVo.setRowNum(rowNum);
            result.put("currentPage", toolVo.getCurrentPage());
            result.put("pageSize", toolVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, toolVo.getPageSize()));
            result.put("rowNum", toolVo.getRowNum());
        }
        return result;
    }


}
