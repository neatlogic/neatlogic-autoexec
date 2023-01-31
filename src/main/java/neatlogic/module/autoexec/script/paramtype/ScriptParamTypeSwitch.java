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
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeSwitch extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.SWITCH.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.SWITCH.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "开关";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 16;
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
                this.put("type", "switch");
            }
        };
    }

    @Override
    public Boolean myNeedDataSource() {
        return false;
    }

    @Override
    public Object getMyTextByValue(Object value) {
        return value;
    }

    @Override
    public Object getMyAutoexecParamByValue(Object value) {
        return getMyTextByValue(value);
    }
}
