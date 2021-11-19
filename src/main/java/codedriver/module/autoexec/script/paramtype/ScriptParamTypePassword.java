/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.script.paramtype;

import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import codedriver.framework.common.util.RC4Util;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypePassword extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.PASSWORD.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.PASSWORD.getText();
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
                this.put("type", "password");
                this.put("maxlength", 50);
                this.put("showPassword", true);
                this.put("placeholder", "请输入");
            }
        };
    }

    @Override
    protected Object getMyTextByValue(Object value) {
        //先解密
        String plain = RC4Util.decrypt(value.toString());
        //再按autoexec 的key 加密
        value = "{ENCRYPTED}" + RC4Util.encrypt(AutoexecJobVo.AUTOEXEC_RC4_KEY, plain);
        return value;
    }

    @Override
    public Object getMyAutoexecParamByValue(Object value){
        return getMyTextByValue(value);
    }
}
