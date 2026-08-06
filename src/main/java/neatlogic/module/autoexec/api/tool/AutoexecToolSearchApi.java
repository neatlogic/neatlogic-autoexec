/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.tool;

import neatlogic.framework.util.$;

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
        return "nmaa.autoexectoolsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", desc = "term.autoexec.execmode"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.typeidlist"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.riskidlist"),
            @Param(name = "customTemplateIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.customtemplateidlist"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "execrtoolAuthorityStatus", type = ApiParamType.ENUM, rule = "authorized,unauthorized", desc = "common.execrtoolauthoritystatus"),
            @Param(name = "execrtoolAuthorityUuidList", type = ApiParamType.JSONARRAY, desc = "common.execrtoolauthorityuuidlist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmaa.common.input.param.desc.needpage")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecToolVo[].class, desc = "nmaa.autoexectoolsearchapi.output.param.desc.tbodylist"),
            @Param(name = "operateList", type = ApiParamType.JSONARRAY, desc = "nmaa.common.output.param.desc.actionlist"),
            @Param(name = "execrtoolAuthorityList", type = ApiParamType.JSONARRAY, desc = "common.executeauthoritylist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmaa.autoexectoolsearchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecToolVo toolVo = JSON.toJavaObject(jsonObj, AutoexecToolVo.class);
        if (CollectionUtils.isNotEmpty(toolVo.getCustomTemplateIdList()) && toolVo.getCustomTemplateIdList().contains(0L)) {
            toolVo.setCustomTemplateId(0L);
            toolVo.setCustomTemplateIdList(null);
        }
        // 先按筛选条件分页获取工具ID，再通过主键批量查询工具及权限明细，避免一对多关联干扰分页。
        List<Long> toolIdList = autoexecToolMapper.searchToolIdList(toolVo);
        List<AutoexecToolVo> toolVoList = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            toolVoList = autoexecToolMapper.getToolListForSearchByIdList(toolIdList);
        }
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
                    active.setDisabledReason($.t("nmar.operate.permissiondenied"));
                }
                if (!hasScriptModifyAuth) {
                    test.setDisabled(1);
                    test.setDisabledReason($.t("nmar.operate.permissiondenied"));
                }
                if (hasCombopAddAuth) {
//                    if (MapUtils.isNotEmpty(hasBeenGeneratedToCombopMap) && Objects.equals(hasBeenGeneratedToCombopMap.get(o.getId()), true)) {
                    if (hasBeenGeneratedToCombopList.contains(o.getId())) {
                        generateToCombop.setDisabled(1);
                        generateToCombop.setDisabledReason($.t("nmar.operate.publishedascombop"));
                    } else if (!Objects.equals(o.getIsActive(), 1)) {
                        generateToCombop.setDisabled(1);
                        generateToCombop.setDisabledReason($.t("nmar.operate.inactivetoolcannotpublish"));
                    }
                } else {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason($.t("nmar.operate.permissiondenied"));
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
