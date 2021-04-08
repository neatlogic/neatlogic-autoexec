/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

public enum ExecMode {
    LOCAL("local", "本地"),
    REMOTE("remote", "远程"),
    LOCALREMOTE("localremote", "本地-远程");
    private String value;
    private String text;

    private ExecMode(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
