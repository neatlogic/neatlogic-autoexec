/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.script.paramtype;

import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author laiwt
 * @since 2022/05/16 15:37
 **/
@Service
public class ScriptParamTypeTextarea extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.TEXTAREA.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.TEXTAREA.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "可输出字符串、数字";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 14;
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
                this.put("type", "textarea");
                this.put("maxlength", 2048);
                this.put("placeholder", "请输入");
            }
        };
    }
}
