/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.startup;

import neatlogic.framework.autoexec.constvalue.AutoexecTypeAuthorityAction;
import neatlogic.framework.autoexec.constvalue.AutoexecTypeType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeAuthVo;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.type.AutoexecTypeFactory;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.common.constvalue.UserType;
import neatlogic.framework.deploy.constvalue.DeployWhiteType;
import neatlogic.framework.startup.StartupBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
class AutoexecTypeInit extends StartupBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getName() {
        return "初始化自动化工具分类";
    }

    @Override
    public void executeForCurrentTenant() {
        JSONArray needInitTypeList = AutoexecTypeFactory.getAutoexecTypeList();
        if (CollectionUtils.isEmpty(needInitTypeList)) {
            return;
        }
        List<AutoexecTypeVo> insertTypeList = new ArrayList<>();
        List<AutoexecTypeAuthVo> insertTypeAuthList = new ArrayList<>();
        List<AutoexecTypeAuthVo> insertTypeReviewAuthList = new ArrayList<>();
        List<String> insertTypeNameList = new ArrayList<>();
        for (int i = 0; i < needInitTypeList.size(); i++) {
            JSONObject autoexecType = needInitTypeList.getJSONObject(i);
            insertTypeNameList.add(autoexecType.getString("value"));
        }
        //查出已经存在且需要初始化的工具分类
        List<AutoexecTypeVo> hasInitTypeList = new ArrayList<>();
        Map<String, AutoexecTypeVo> hasInitTypeNameMap = new HashMap<>();
        Map<Long, List<AutoexecTypeAuthVo>> hasReviewAuthInitTypeIdMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(insertTypeNameList)) {
            hasInitTypeList = autoexecTypeMapper.getTypeListByNameList(insertTypeNameList);
            hasInitTypeNameMap = hasInitTypeList.stream().collect(Collectors.toMap(AutoexecTypeVo::getName, e -> e));
            List<Long> hasInitTypeIdList = hasInitTypeList.stream().map(AutoexecTypeVo::getId).collect(toList());
            if(CollectionUtils.isNotEmpty(hasInitTypeIdList)) {
                List<AutoexecTypeAuthVo> reviewAuthList = autoexecTypeMapper.getAutoexecTypeAuthListByTypeIdListAndAction(hasInitTypeIdList, AutoexecTypeAuthorityAction.REVIEW.getValue());
                for (AutoexecTypeAuthVo authVo : reviewAuthList) {
                    hasReviewAuthInitTypeIdMap.computeIfAbsent(authVo.getTypeId(), key -> new ArrayList<>()).add(authVo);
                }
            }
        }

        List<Long> insertDeployActiveTypeId = new ArrayList<>();
        for (int i = 0; i < needInitTypeList.size(); i++) {
            JSONObject autoexecType = needInitTypeList.getJSONObject(i);
            String typeName = autoexecType.getString("value");
            if (hasInitTypeNameMap.containsKey(typeName)) {
                //已存在的分类若没有数据权限，则需要初始化数据权限
                AutoexecTypeVo hasExistTypeVo = hasInitTypeNameMap.get(typeName);
                if (CollectionUtils.isEmpty(hasExistTypeVo.getAutoexecTypeAuthList())) {
                    insertTypeAuthList.add(new AutoexecTypeAuthVo(hasExistTypeVo.getId(), AutoexecTypeAuthorityAction.ADD.getValue(), GroupSearch.COMMON.getValue(), UserType.ALL.getValue()));
                }
                if (CollectionUtils.isEmpty(hasReviewAuthInitTypeIdMap.get(hasExistTypeVo.getId()))) {
                    insertTypeReviewAuthList.add(new AutoexecTypeAuthVo(hasExistTypeVo.getId(), AutoexecTypeAuthorityAction.REVIEW.getValue(), GroupSearch.COMMON.getValue(), UserType.ALL.getValue()));
                }
                //发布模块工具分类白名单
                if (DeployWhiteType.getValueList().contains(typeName)) {
                    insertDeployActiveTypeId.add(hasExistTypeVo.getId());
                }
            } else {
                AutoexecTypeVo typeVo = new AutoexecTypeVo();
                typeVo.setId(autoexecType.getLong("id"));
                typeVo.setDescription(autoexecType.getString("text"));
                typeVo.setName(typeName);
                typeVo.setLcu(SystemUser.SYSTEM.getUserUuid());
                typeVo.setType(AutoexecTypeType.FACTORY.getValue());
                insertTypeList.add(typeVo);

                //发布模块工具分类白名单
                if (DeployWhiteType.getValueList().contains(typeName)) {
                    insertDeployActiveTypeId.add(typeVo.getId());
                }
            }
        }
        if (CollectionUtils.isNotEmpty(insertTypeList)) {
            autoexecTypeMapper.insertTypeList(insertTypeList);
            List<Long> typeIdList = insertTypeList.stream().map(AutoexecTypeVo::getId).collect(toList());
            List<Long> hasAuthTypeIdList = autoexecTypeMapper.getHasAuthTypeIdListByTypeIdList(typeIdList);
            List<Long> needInsertAuthList = CollectionUtils.isNotEmpty(hasAuthTypeIdList) ? typeIdList.stream().filter(item -> !hasAuthTypeIdList.contains(item)).collect(toList()) : typeIdList;
            if (CollectionUtils.isNotEmpty(needInsertAuthList)) {
                autoexecTypeMapper.insertBatchTypeAuth(needInsertAuthList, AutoexecTypeAuthorityAction.ADD.getValue(), GroupSearch.COMMON.getValue(), UserType.ALL.getValue());
                autoexecTypeMapper.insertBatchTypeAuth(needInsertAuthList,  AutoexecTypeAuthorityAction.REVIEW.getValue(), GroupSearch.COMMON.getValue(), UserType.ALL.getValue());
            }
        }
        if (CollectionUtils.isNotEmpty(insertTypeAuthList)) {
            autoexecTypeMapper.insertTypeAuthList(insertTypeAuthList);
        }
        if (CollectionUtils.isNotEmpty(insertTypeReviewAuthList)) {
            autoexecTypeMapper.insertTypeAuthList(insertTypeReviewAuthList);
        }
        if (CollectionUtils.isNotEmpty(insertDeployActiveTypeId)) {
            autoexecTypeMapper.insertDeployActiveList(insertDeployActiveTypeId, 1);
        }
    }

    @Override
    public void executeForAllTenant() {

    }

    @Override
    public int sort() {
        return 0;
    }
}
