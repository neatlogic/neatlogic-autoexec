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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopSearchVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.cmdb.enums.CmdbTenantConfig;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询组合工具列表接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@AuthAction(action = AUTOEXEC.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/list";
    }

    @Override
    public String getName() {
        return "查询组合工具列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询，支持名称或唯一标识"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "状态"),
            @Param(name = "versionStatus", type = ApiParamType.ENUM, rule = "draft,submitted,passed,rejected", desc = "状态"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecCombopVo[].class, desc = "组合工具列表")
    })
    @Description(desc = "查询组合工具列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        AutoexecCombopSearchVo searchVo = jsonObj.toJavaObject(AutoexecCombopSearchVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<AutoexecCombopVo> autoexecCombopList = autoexecCombopMapper.getAutoexecCombopByIdList(idList);
            autoexecCombopList.sort(Comparator.comparingInt(o -> idList.indexOf(o.getId())));
            resultObj.put("tbodyList", autoexecCombopList);
            return resultObj;
        }
        String versionStatus = searchVo.getVersionStatus();
        if (StringUtils.isBlank(versionStatus)) {
            versionStatus = ScriptVersionStatus.PASSED.getValue();
        }
        List<String> versionStatusList = Arrays.asList(
                ScriptVersionStatus.PASSED.getValue(),
                ScriptVersionStatus.DRAFT.getValue(),
                ScriptVersionStatus.SUBMITTED.getValue(),
                ScriptVersionStatus.REJECTED.getValue()
        );
        searchVo.setUserUuid(UserContext.get().getUserUuid());
        List<String> uuidList = new ArrayList<>();
        uuidList.add(UserContext.get().getUserUuid());
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        if (CollectionUtils.isNotEmpty(authenticationInfoVo.getTeamUuidList())) {
            uuidList.addAll(authenticationInfoVo.getTeamUuidList());
        }
        if (CollectionUtils.isNotEmpty(authenticationInfoVo.getRoleUuidList())) {
            uuidList.addAll(authenticationInfoVo.getRoleUuidList());
        }
        searchVo.setUuidList(uuidList);
        if (AuthActionChecker.check(AUTOEXEC_MODIFY.class)) {
            searchVo.setIsHasAllAuthority(1);
        } else {
            searchVo.setIsHasAllAuthority(0);
        }
        Map<String, Integer> versionStatusCountMap = new HashMap<>();
        for (String status : versionStatusList) {
            searchVo.setVersionStatus(status);
            int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
            if (rowNum > 0) {
                if (Objects.equals(status, versionStatus)) {
                    searchVo.setRowNum(rowNum);
                    Map<Object, Integer> countMap = new HashMap<>();
                    List<AutoexecCombopVo> autoexecCombopList = new ArrayList<>();
                    List<Long> idList = autoexecCombopMapper.getAutoexecCombopIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        autoexecCombopList = autoexecCombopMapper.getAutoexecCombopByIdList(idList);
                        autoexecCombopList.sort(Comparator.comparingInt(o -> idList.indexOf(o.getId())));
                        countMap = DependencyManager.getBatchDependencyCount(AutoexecFromType.COMBOP, idList);
                    }
                    for (AutoexecCombopVo autoexecCombopVo : autoexecCombopList) {
                        AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(autoexecCombopVo.getTypeId());
                        if (autoexecTypeVo != null) {
                            autoexecCombopVo.setTypeName(autoexecTypeVo.getName());
                        }
                        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
                        Integer count = countMap.get(autoexecCombopVo.getId().toString());
                        if (count == null) {
                            count = 0;
                        }
                        autoexecCombopVo.setReferenceCount(count);
                    }
                    resultObj.put("tbodyList", autoexecCombopList);
                }
                versionStatusCountMap.put(status, rowNum);
            } else {
                versionStatusCountMap.put(status, 0);
            }
        }
        resultObj.put("rowNum", searchVo.getRowNum());
        resultObj.put("pageCount", searchVo.getPageCount());
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", searchVo.getPageSize());
        resultObj.put("versionStatusCountMap", versionStatusCountMap);
        resultObj.put("isResourcecenterAuth", ConfigManager.getConfig(CmdbTenantConfig.IS_RESOURCECENTER_AUTH));
        return resultObj;
    }
}
