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

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.OperateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.operate.ScriptOperateManager;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/search";
    }

    @Override
    public String getName() {
        return "nmaa.autoexecscriptsearchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", desc = "term.autoexec.execmode"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.typeidlist"),
            @Param(name = "catalogId", type = ApiParamType.LONG, desc = "term.autoexec.catalogid"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.riskidlist"),
            @Param(name = "excludeList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecscriptsearchapi.input.param.desc.excludelist"),
            @Param(name = "isLib", type = ApiParamType.INTEGER, desc = "nmaa.autoexecscriptsearchapi.input.param.desc.islib"),
            @Param(name = "customTemplateIdList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.customtemplateidlist"),
            @Param(name = "versionStatus", type = ApiParamType.ENUM, rule = "draft,submitted,passed,rejected", desc = "term.autoexec.versionstatus"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecscriptsearchapi.input.param.desc.defaultvalue"),
            @Param(name = "execrtoolAuthorityStatus", type = ApiParamType.ENUM, rule = "authorized,unauthorized", desc = "common.execrtoolauthoritystatus"),
            @Param(name = "execrtoolAuthorityUuidList", type = ApiParamType.JSONARRAY, desc = "common.execrtoolauthorityuuidlist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmaa.common.input.param.desc.needpage")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVo[].class, desc = "nmaa.autoexecscriptsearchapi.output.param.desc.tbodylist"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "nmaa.autoexecscriptsearchapi.output.param.desc.statuslist"),
            @Param(name = "operateList", type = ApiParamType.JSONARRAY, desc = "nmaa.common.output.param.desc.actionlist"),
            @Param(name = "execrtoolAuthorityList", type = ApiParamType.JSONARRAY, desc = "common.executeauthoritylist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmaa.autoexecscriptsearchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        if (CollectionUtils.isNotEmpty(scriptVo.getCustomTemplateIdList()) && scriptVo.getCustomTemplateIdList().contains(0L)) {
            scriptVo.setCustomTemplateId(0L);
            scriptVo.setCustomTemplateIdList(null);
        }
        //查询各级子目录
        scriptVo.setCatalogIdList(autoexecScriptService.getCatalogIdList(scriptVo.getCatalogId()));

        scriptVo.setIsLib(jsonObj.getInteger("isLib") != null ? jsonObj.getInteger("isLib") : null);
        // 先按筛选条件分页获取脚本ID，再通过主键批量查询脚本及权限明细，避免一对多关联干扰分页。
        List<Long> scriptIdList = autoexecScriptMapper.searchScriptIdList(scriptVo);
        List<AutoexecScriptVo> scriptVoList = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            scriptVoList = autoexecScriptMapper.getScriptListForSearchByIdList(scriptIdList);
        }
        if (!scriptVoList.isEmpty()) {
            List<AutoexecCatalogVo> catalogList = autoexecCatalogMapper.getCatalogListByIdList(scriptVoList.stream().map(AutoexecScriptVo::getCatalogId).collect(Collectors.toList()));
            Map<Long, AutoexecCatalogVo> catalogMap = catalogList.stream().collect(Collectors.toMap(AutoexecCatalogVo::getId, o -> o));
            if (MapUtils.isNotEmpty(catalogMap)) {
                for (AutoexecScriptVo vo : scriptVoList) {
                    AutoexecCatalogVo catalog = catalogMap.get(vo.getCatalogId());
                    if (catalog != null) {
                        vo.setCatalogName(catalog.getName());
                        List<AutoexecCatalogVo> upwardList = autoexecCatalogMapper.getParentListAndSelfByLR(catalog.getLft(), catalog.getRht());
                        vo.setCatalogPath(upwardList.stream().map(AutoexecCatalogVo::getName).collect(Collectors.joining("/")));
                    }
                }
            }
            if (StringUtils.isNotBlank(scriptVo.getVersionStatus())) {
                List<AutoexecScriptVersionVo> parserList = autoexecScriptMapper.getVersionParserByScriptIdListAndVersionStatus(scriptVoList.stream().map(AutoexecScriptVo::getId).collect(Collectors.toList()), scriptVo.getVersionStatus());
                if (!parserList.isEmpty()) {
                    Map<Long, String> collect = parserList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, AutoexecScriptVersionVo::getParser));
                    for (AutoexecScriptVo vo : scriptVoList) {
                        vo.setParser(collect.get(vo.getId()));
                    }
                }
            }
            //补是否作为库文件被其他工具依赖
            List<AutoexecScriptVo> libScriptList = scriptVoList.stream().filter(e -> Objects.equals(e.getIsLib(), 1)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(libScriptList)) {
                List<Long> libScriptIdList = libScriptList.stream().map(AutoexecScriptVo::getId).collect(Collectors.toList());
                List<Long> isLibReferenceList = autoexecScriptMapper.getIsLibReferenceScriptIdByScriptIdList(libScriptIdList);
                for (AutoexecScriptVo vo : scriptVoList) {
                    if (isLibReferenceList.contains(vo.getId())) {
                        vo.setIsLibReference(1);
                    }
                }
            }
        }
        result.put("tbodyList", scriptVoList);
        // 获取操作权限
        if (Objects.equals(ScriptVersionStatus.PASSED.getValue(), scriptVo.getVersionStatus()) && CollectionUtils.isNotEmpty(scriptVoList)) {
            List<Long> idList = scriptVoList.stream().map(AutoexecScriptVo::getId).collect(Collectors.toList());
            Map<Object, Integer> countMap = DependencyManager.getBatchDependencyCount(AutoexecFromType.SCRIPT, idList);
            scriptVoList.forEach(o -> o.setReferenceCount(countMap.get(o.getId().toString()) != null ? countMap.get(o.getId().toString()) : 0));
            ScriptOperateManager.Builder builder = new ScriptOperateManager().new Builder();
            builder.addScriptId(idList.toArray(new Long[idList.size()]));
            Map<Long, List<OperateVo>> operateListMap = builder.managerBuild().getOperateListMap();
            if (MapUtils.isNotEmpty(operateListMap)) {
                scriptVoList.forEach(o -> o.setOperateList(operateListMap.get(o.getId())));
            }
        }
        if (scriptVo.getNeedPage()) {
            int rowNum = autoexecScriptMapper.searchScriptCount(scriptVo);
            scriptVo.setRowNum(rowNum);
            result.put("currentPage", scriptVo.getCurrentPage());
            result.put("pageSize", scriptVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, scriptVo.getPageSize()));
            result.put("rowNum", scriptVo.getRowNum());
        }
        // 分别查询含有已通过、草稿、待审批、已驳回状态的脚本数量
        JSONArray statusList = new JSONArray();
        scriptVo.setVersionStatus(ScriptVersionStatus.PASSED.getValue());
        statusList.add(new JSONObject() {{
            this.put("text", ScriptVersionStatus.PASSED.getText());
            this.put("value", ScriptVersionStatus.PASSED.getValue());
            this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
        }});
        if (AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            scriptVo.setVersionStatus(ScriptVersionStatus.DRAFT.getValue());
            statusList.add(new JSONObject() {{
                this.put("text", ScriptVersionStatus.DRAFT.getText());
                this.put("value", ScriptVersionStatus.DRAFT.getValue());
                this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
            }});
            scriptVo.setVersionStatus(ScriptVersionStatus.SUBMITTED.getValue());
            statusList.add(new JSONObject() {{
                this.put("text", ScriptVersionStatus.SUBMITTED.getText());
                this.put("value", ScriptVersionStatus.SUBMITTED.getValue());
                this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
            }});
            scriptVo.setVersionStatus(ScriptVersionStatus.REJECTED.getValue());
            statusList.add(new JSONObject() {{
                this.put("text", ScriptVersionStatus.REJECTED.getText());
                this.put("value", ScriptVersionStatus.REJECTED.getValue());
                this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
            }});
        }
        result.put("statusList", statusList);
        return result;
    }


}
