/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

/**
 * @author: linbq
 * @since: 2021/4/13 14:43
 **/
public enum CombopOperationType {
    COMBOP("combop", "组合"),
    SCRIPT("script", "脚本"),
    TOOL("tool", "工具");

    private CombopOperationType(String value, String text) {
        this.value = value;
        this.text = text;
    }

    private String value;
    private String text;

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
