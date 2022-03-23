/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

/**
 * @author linbq
 * @since 2021/9/2 14:40
 **/
public enum AutoexecJobGroupPolicy {
    ONESHOT("oneshot"),
    GRAYSCALE("grayscale");
    private final String name;

    AutoexecJobGroupPolicy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
