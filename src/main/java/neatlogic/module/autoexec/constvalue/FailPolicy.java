package neatlogic.module.autoexec.constvalue;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.common.constvalue.IEnum;
import neatlogic.framework.util.$;

import java.util.List;

public enum FailPolicy implements IEnum {
    HANG("hang", "nmacv.failpolicy.text.hang"),
    KEEP_ON("keepon", "nmacv.failpolicy.text.keep_on")
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
        return $.t(name);
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
