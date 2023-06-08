package neatlogic.module.autoexec.constvalue;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.type.IAutoexecType;
import neatlogic.framework.util.$;

public enum AutoexecType implements IAutoexecType {
    TEST(3L, "TEST", "测试用工具"),
    INSTALL(4L, "INSTALL", "软件安装配置"),
    START_STOP(5L, "START_STOP", "启停操作"),
    NATIVE(6L, "NATIVE", "调度器内置工具"),
    BASIC(7L, "BASIC", "基础工具"),
    TEMP(8L, "TEMP", "临时使用"),
    BIZ_JOBS(10L, "BIZ_JOBS", "作业调度"),
    BACKUP(12L, "BACKUP", "备份"),
    SQL_FILE(13L, "SQL_FILE", "SQL处理"),
    DR_SWITCH(14L, "DR_SWITCH", "灾备切换"),
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
