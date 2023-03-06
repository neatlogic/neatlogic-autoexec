package neatlogic.module.autoexec.constvalue;

import neatlogic.framework.autoexec.type.IAutoexecType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.util.I18nUtils;

public enum AutoexecType implements IAutoexecType {
    TEST(3L, "TEST", "enum.autoexec.autoexectype.test"),
    INSTALL(4L, "INSTALL", "enum.autoexec.autoexectype.install"),
    START_STOP(5L, "START_STOP", "enum.autoexec.autoexectype.start_stop"),
    NATIVE(6L, "NATIVE", "enum.autoexec.autoexectype.native"),
    BASIC(7L, "BASIC", "enum.autoexec.autoexectype.basic"),
    TEMP(8L, "TEMP", "enum.autoexec.autoexectype.temp"),
    BIZ_JOBS(10L, "BIZ_JOBS", "enum.autoexec.autoexectype.biz_jobs"),
    BACKUP(12L, "BACKUP", "enum.autoexec.autoexectype.backup"),
    SQL_FILE(13L, "SQL_FILE", "enum.autoexec.autoexectype.sql_file"),
    DR_SWITCH(14L, "DR_SWITCH", "enum.autoexec.autoexectype.dr_switch"),
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
        return I18nUtils.getMessage(text);
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
