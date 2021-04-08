/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

public enum ScriptParser {
    PYTHON("python"),
    VBS("vbs"),
    SHELL("shell"),
    PERL("perl"),
    POWERSHELL("powershell"),
    BAT("bat"),
    XML("xml");
    private String value;

    private ScriptParser(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
