/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.script.paramtype;

import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author linbq
 * @since 2022/05/19 17:17
 **/
@Service
public class ScriptParamTypePhase extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.PHASE.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.PHASE.getText();
    }

    /**
     * 获取参数描述
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "可选择阶段";
    }

    /**
     * 排序
     *
     * @return
     */
    @Override
    public int getSort() {
        return 15;
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
                this.put("type", "phase");
                this.put("placeholder", "请选择");
                this.put("multiple", false);
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
    public Object getMyAutoexecParamByValue(Object value){
        return getMyTextByValue(value);
    }
}
