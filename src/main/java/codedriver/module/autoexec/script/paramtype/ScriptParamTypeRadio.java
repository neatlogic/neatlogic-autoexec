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
 * @author lvzk
 * @since 2021/11/18 15:37
 **/
@Service
public class ScriptParamTypeRadio extends ScriptParamTypeBase {
    /**
     * 获取参数类型
     *
     * @return 类型
     */
    @Override
    public String getType() {
        return ParamType.RADIO.getValue();
    }

    /**
     * 获取参数类型名
     *
     * @return 类型名
     */
    @Override
    public String getTypeName() {
        return ParamType.RADIO.getText();
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
                this.put("type", "radio");
                this.put("placeholder", "请选择");
                this.put("url", "/api/rest/matrix/column/data/search/forselect/new");
                this.put("rootName", "columnDataList");
            }
        };
    }

    @Override
    public Boolean myNeedDataSource(){
        return true;
    }

    @Override
    public Object getMyTextByValue(Object value) {
        String valueStr = value.toString();
        int tmpIndex = valueStr.indexOf("&=&");
        if (tmpIndex > -1) {
            return valueStr.substring(tmpIndex + 3);
        }
        return value;
    }

    @Override
    public Object getMyAutoexecParamByValue(Object value){
        return getMyTextByValue(value);
    }
}
