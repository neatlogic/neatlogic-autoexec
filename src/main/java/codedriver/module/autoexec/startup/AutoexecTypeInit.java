/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.startup;

import codedriver.framework.autoexec.constvalue.AutoexecTypeType;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeAuthVo;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.type.AutoexecTypeFactory;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.common.constvalue.UserType;
import codedriver.framework.startup.StartupBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        for (int i = 0; i < autoexecTypeList.size(); i++) {
            JSONObject autoexecType = autoexecTypeList.getJSONObject(i);
            autoexecType.getString("value");
            AutoexecTypeVo typeVo = new AutoexecTypeVo();
            typeVo.setId(autoexecType.getLong("id"));
            typeVo.setDescription(autoexecType.getString("text"));
            typeVo.setName(autoexecType.getString("value"));
            typeVo.setLcu(SystemUser.SYSTEM.getUserUuid());
            typeVo.setType(AutoexecTypeType.FACTORY.getValue());
            autoexecTypeMapper.insertType(typeVo);
            autoexecTypeMapper.insertTypeAuth(new AutoexecTypeAuthVo(typeVo.getId(), GroupSearch.COMMON.getValue(), UserType.ALL.getValue()));
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
