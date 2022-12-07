/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.config;

import codedriver.framework.common.config.IConfigListener;

import java.util.Properties;

public class AutoexecConfig implements IConfigListener {
    private static String PROXY_BASIC_USER_NAME;
    private static String PROXY_BASIC_PASSWORD;
    private static Boolean AUTOEXEC_JOB_IS_ALLOWED_MANUAL_TRIGGER_IN_ADVANCE; // 是否允许人工提前触发自动化作业

    public static String PROXY_BASIC_USER_NAME() {
        return PROXY_BASIC_USER_NAME;
    }

    public static String PROXY_BASIC_PASSWORD() {
        return PROXY_BASIC_PASSWORD;
    }

    public static Boolean AUTOEXEC_JOB_IS_ALLOWED_MANUAL_TRIGGER_IN_ADVANCE() {
        return AUTOEXEC_JOB_IS_ALLOWED_MANUAL_TRIGGER_IN_ADVANCE;
    }


    @Override
    public void loadConfig(Properties prop) {
        PROXY_BASIC_USER_NAME = prop.getProperty("proxy.basic.username", "codedriver");
        PROXY_BASIC_PASSWORD = prop.getProperty("proxy.basic.password", "123456");
        AUTOEXEC_JOB_IS_ALLOWED_MANUAL_TRIGGER_IN_ADVANCE = Boolean.valueOf(prop.getProperty("autoexec.job.isallowed.manualtrigger.inadvance", "true"));
    }
}
