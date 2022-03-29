/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

import codedriver.framework.common.constvalue.IEnum;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author lvzk
 * @since 2022/3/29 14:40
 **/
public enum AutoexecJobPhaseExecutePolicy implements IEnum {
    FIRST_ROUND("firstRound"),
    MIDDLE_ROUND("middleRound"),
    LAST_ROUND("lastRound");
    private final String name;

    AutoexecJobPhaseExecutePolicy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public List getValueTextList() {
        JSONArray resultList = new JSONArray();
        for (AutoexecJobPhaseExecutePolicy e : values()) {
            JSONObject obj = new JSONObject();
            obj.put("value", e.getName());
            obj.put("text", e.getName());
            resultList.add(obj);
        }
        return resultList;
    }
}
