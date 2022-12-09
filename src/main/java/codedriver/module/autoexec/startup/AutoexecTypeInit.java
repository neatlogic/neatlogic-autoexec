/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.startup;

import codedriver.framework.autoexec.constvalue.AutoexecTypeType;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.type.AutoexecTypeFactory;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.common.constvalue.UserType;
import codedriver.framework.startup.StartupBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
class InspectAutoexecTypeInit extends StartupBase {

    @Resource

    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getName() {
        return "添加巡检的工具分类";
    }

    @Override
    public void executeForCurrentTenant() {
        JSONArray autoexecTypeList = AutoexecTypeFactory.getAutoexecTypeList();
        List<AutoexecTypeVo> insertTypeList = new ArrayList<>();
        for (int i = 0; i < autoexecTypeList.size(); i++) {
            JSONObject autoexecType = autoexecTypeList.getJSONObject(i);
            autoexecType.getString("value");
            AutoexecTypeVo typeVo = new AutoexecTypeVo();
            typeVo.setId(autoexecType.getLong("id"));
            typeVo.setDescription(autoexecType.getString("text"));
            typeVo.setName(autoexecType.getString("value"));
            typeVo.setLcu(SystemUser.SYSTEM.getUserUuid());
            typeVo.setType(AutoexecTypeType.FACTORY.getValue());
            insertTypeList.add(typeVo);
        }
        if (CollectionUtils.isNotEmpty(insertTypeList)) {
            autoexecTypeMapper.insertTypeList(insertTypeList);
            List<Long> typeIdList = insertTypeList.stream().map(AutoexecTypeVo::getId).collect(toList());
            List<Long> hasAuthTypeIdList = autoexecTypeMapper.getHasAuthTypeIdListByTypeIdList(typeIdList);
            List<Long> needInsertAuthList = CollectionUtils.isNotEmpty(hasAuthTypeIdList) ? typeIdList.stream().filter(item -> !hasAuthTypeIdList.contains(item)).collect(toList()) : typeIdList;
            if (CollectionUtils.isNotEmpty(needInsertAuthList)) {
                autoexecTypeMapper.insertBatchTypeAuth(needInsertAuthList, GroupSearch.COMMON.getValue(), UserType.ALL.getValue());
            }
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
