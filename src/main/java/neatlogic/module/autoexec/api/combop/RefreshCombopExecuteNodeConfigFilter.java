/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.NoAuth;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 临时刷新数据用，后续废除
 */
@Service
@AuthAction(action = NoAuth.class)
public class RefreshCombopExecuteNodeConfigFilter extends PrivateApiComponentBase {

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVerMapper;

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具ID")
    })
    @Description(desc = "刷新历史执行时再指定执行目标的过滤器到前置过滤器")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long combopId = paramObj.getLong("combopId");
        if (combopId != null) {
            AutoexecCombopVersionVo activeCombopVersionVo = autoexecCombopVerMapper.getAutoexecCombopActiveVersionByCombopId(combopId);
            if (activeCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(combopId.toString());
            }
            exchange(activeCombopVersionVo);
        } else {
            //更新所有组合工具
            BasePageVo basePageVo = new BasePageVo();
            int rowNum = autoexecCombopVerMapper.getAutoexecCombopVersionCountForUpdateConfig();
            basePageVo.setPageSize(100);
            basePageVo.setRowNum(rowNum);
            if (rowNum > 0) {
                for (int i = 0; i < basePageVo.getPageSize(); i++) {
                    basePageVo.setCurrentPage(i+1);
                    List<Map<String, Object>> versionList = autoexecCombopVerMapper.getAutoexecCombopVersionListForUpdateConfig(basePageVo);
                    for (Map<String, Object> versionVo : versionList) {
                        Long id = (Long) versionVo.get("id");
                        String configStr = (String) versionVo.get("config");
                        if (StringUtils.isBlank(configStr)) {
                            continue;
                        }
                        JSONObject config = null;
                        try {
                            config = JSONObject.parseObject(configStr);
                        } catch (JSONException e) {
                            //System.out.println("格式不对");
                        }
                        if (MapUtils.isEmpty(config)) {
                            continue;
                        }
                        AutoexecCombopVersionVo autoexecCombopVersionVo = new AutoexecCombopVersionVo();
                        autoexecCombopVersionVo.setId(id);
                        autoexecCombopVersionVo.setConfigStr(configStr);
                        exchange(autoexecCombopVersionVo);
                    }
                }

            }
        }

        return null;
    }

    /**
     * 将历史执行时再指定执行目标的过滤器迁移到前置过滤器
     *
     * @param versionVo 组合工具版本
     */
    private void exchange(AutoexecCombopVersionVo versionVo) {
        AutoexecCombopVersionConfigVo configVo = versionVo.getConfig();
        if (configVo != null && configVo.getExecuteConfig() != null && configVo.getExecuteConfig().getExecuteNodeConfig() != null && MapUtils.isNotEmpty(configVo.getExecuteConfig().getExecuteNodeConfig().getFilter())) {
            configVo.getExecuteConfig().setPreCondition(configVo.getExecuteConfig().getExecuteNodeConfig().getFilter());
            configVo.getExecuteConfig().getExecuteNodeConfig().setFilter(null);
        }
        autoexecCombopVerMapper.updateAutoexecCombopVersionConfigById(versionVo.getId(), JSONObject.toJSONString(configVo));
    }

    @Override
    public String getToken() {
        return "combop/executenodeconfig/filter/refresh";
    }

    @Override
    public String getName() {
        return "刷新历史执行时再指定执行目标的过滤器";
    }
}
