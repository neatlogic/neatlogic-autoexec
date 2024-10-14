/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.script.paramtype;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeAccount extends ScriptParamTypeBase {

    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.ACCOUNT.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.ACCOUNT.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "服务的连接协议、账号，用户连接主机上的数据库，中间件等服务";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 12;
    }

    /**
     * 获取前端初始化配置
     *
     * @return 配置
     */
    @Override
    public JSONObject getConfig() {
        return new JSONObject() {
            {
                this.put("type", "account");
                this.put("placeholder", "请选择");
            }
        };
    }

    @Override
    public Object getMyAutoexecParamByValue(Object value) {
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountVo accountVo = resourceAccountCrossoverMapper.getAccountById(Long.valueOf(value.toString()));
            if (accountVo != null) {
                value = accountVo.getAccount() + "/" + accountVo.getId() + "/" + accountVo.getProtocol();
            }
        }
        return value;
    }

    @Override
    public Object getMyExchangeParamByValue(Object value) {
        Long accountId = null;
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            try {
                accountId = Long.valueOf(value.toString());
            } catch (NumberFormatException ignored) {

            }

            if (accountId == null) {
                AccountVo accountVo = resourceAccountCrossoverMapper.getPublicAccountByName(value.toString());
                if(accountVo != null) {
                    accountId = accountVo.getId();
                }
            }

        }
        return accountId;
    }

    @Override
    protected Object getMyTextByValue(Object value, JSONObject config) {
        String valueStr = value.toString();
        if (StringUtils.isNotBlank(valueStr)) {
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountVo accountVo = resourceAccountCrossoverMapper.getAccountById(Long.valueOf(valueStr));
            if (accountVo != null) {
                valueStr = accountVo.getName() + "(" + accountVo.getAccount() + "/" + accountVo.getProtocol() + ")";
            }
        }
        return valueStr;
    }

    @Override
    public Object convertDataForProcessComponent(JSONArray jsonArray) {
        // 账号id，单选
        return getAccountId(jsonArray);
    }
}
