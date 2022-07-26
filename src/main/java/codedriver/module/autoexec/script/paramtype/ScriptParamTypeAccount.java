/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.script.paramtype;

import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import codedriver.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        return "服务的连接协议、帐号，用户连接主机上的数据库，中间件等服务";
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
    protected Object getMyTextByValue(Object value) {
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
}
