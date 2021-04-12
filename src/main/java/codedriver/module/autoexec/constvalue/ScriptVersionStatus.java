/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

public enum ScriptVersionStatus {
    DRAFT("draft", "编辑中", ""),
    SUBMITTED("submitted", "待审批", "#F59A23"),
    PASSED("passed", "已通过", "#00BCD4"),
    REJECTED("rejected", "已驳回", "#D9001B");
    private String value;
    private String text;
    private String color;

    private ScriptVersionStatus(String value, String text, String color) {
        this.value = value;
        this.text = text;
        this.color = color;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public String getColor() {
        return color;
    }

    public static String getText(String _value) {
        for (ScriptVersionStatus status : values()) {
            if (status.value.equals(_value)) {
                return status.text;
            }
        }
        return "";
    }

    public static String getColor(String _value) {
        for (ScriptVersionStatus status : values()) {
            if (status.value.equals(_value)) {
                return status.color;
            }
        }
        return "";
    }

}
