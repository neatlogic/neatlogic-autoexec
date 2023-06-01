package neatlogic.module.autoexec.constvalue;

import neatlogic.framework.common.constvalue.IEnum;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.util.I18nUtils;

import java.util.List;

public enum FailPolicy implements IEnum {
    HANG("hang", "人工处理"),
    KEEP_ON("keepon", "向后流转")
    ;

    private String value;
    private String name;

    FailPolicy(String _value, String _name) {
        this.value = _value;
        this.name = _name;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return I18nUtils.getMessage(name);
    }

    @Override
    public List getValueTextList() {
        JSONArray array = new JSONArray();
        for (FailPolicy s : values()) {
            JSONObject json = new JSONObject();
            json.put("value", s.getValue());
            json.put("text", s.getText());
            array.add(json);
        }
        return array;
    }
}
