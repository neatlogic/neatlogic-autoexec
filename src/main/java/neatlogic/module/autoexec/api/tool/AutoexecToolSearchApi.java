/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.ScriptAndToolOperate;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.OperateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
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
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "customTemplateIdList", type = ApiParamType.JSONARRAY, desc = "自定义模版ID列表"),
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
        if (CollectionUtils.isNotEmpty(toolVo.getCustomTemplateIdList()) && toolVo.getCustomTemplateIdList().contains(0L)) {
            toolVo.setCustomTemplateId(0L);
            toolVo.setCustomTemplateIdList(null);
        }
        List<AutoexecToolVo> toolVoList = autoexecToolMapper.searchTool(toolVo);
        result.put("tbodyList", toolVoList);
        if (CollectionUtils.isNotEmpty(toolVoList)) {
            List<Long> idList = toolVoList.stream().map(AutoexecToolVo::getId).collect(Collectors.toList());
            List<Long> hasBeenGeneratedToCombopList = autoexecToolMapper.checkToolListHasBeenGeneratedToCombop(idList);
            Map<Long, Boolean> hasBeenGeneratedToCombopMap = new HashMap<>();
//            if (CollectionUtils.isNotEmpty(hasBeenGeneratedToCombopList)) {
//                hasBeenGeneratedToCombopList.stream().forEach(o -> hasBeenGeneratedToCombopMap.put(o.getId(), o.getHasBeenGeneratedToCombop() > 0 ? true : false));
//            }
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
//                    if (MapUtils.isNotEmpty(hasBeenGeneratedToCombopMap) && Objects.equals(hasBeenGeneratedToCombopMap.get(o.getId()), true)) {
                    if (hasBeenGeneratedToCombopList.contains(o.getId())) {
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
