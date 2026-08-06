package neatlogic.module.autoexec.constvalue;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.type.IAutoexecType;
import neatlogic.framework.util.$;

public enum AutoexecType implements IAutoexecType {
    TEST(3L, "TEST", "nmacv.autoexectype.text.test"),
    INSTALL(4L, "INSTALL", "nmacv.autoexectype.text.install"),
    START_STOP(5L, "START_STOP", "nmacv.autoexectype.text.start_stop"),
    NATIVE(6L, "NATIVE", "nmacv.autoexectype.text.native"),
    BASIC(7L, "BASIC", "nmacv.autoexectype.text.basic"),
    TEMP(8L, "TEMP", "nmacv.autoexectype.text.temp"),
    BIZ_JOBS(10L, "BIZ_JOBS", "nmacv.autoexectype.text.biz_jobs"),
    BACKUP(12L, "BACKUP", "nmacv.autoexectype.text.backup"),
    SQL_FILE(13L, "SQL_FILE", "nmacv.autoexectype.text.sql_file"),
    DR_SWITCH(14L, "DR_SWITCH", "nmacv.autoexectype.text.dr_switch"),
    ;

    private final Long id;
    private final String value;
    private final String text;

    AutoexecType(Long id, String _value, String _text) {
        this.id = id;
        this.value = _value;
        this.text = _text;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return $.t(text);
    }

    @Override
    public JSONArray getValueTextList() {
        JSONArray array = new JSONArray();
        for (AutoexecType s : values()) {
            JSONObject json = new JSONObject();
            json.put("id", s.getId());
            json.put("value", s.getValue());
            json.put("text", s.getText());
            array.add(json);
        }
        return array;
    }
}
