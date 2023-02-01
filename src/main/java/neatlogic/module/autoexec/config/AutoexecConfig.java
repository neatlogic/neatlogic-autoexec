/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.config;

import neatlogic.framework.common.config.IConfigListener;

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
        PROXY_BASIC_USER_NAME = prop.getProperty("proxy.basic.username", "neatlogic");
        PROXY_BASIC_PASSWORD = prop.getProperty("proxy.basic.password", "123456");
        AUTOEXEC_JOB_IS_ALLOWED_MANUAL_TRIGGER_IN_ADVANCE = Boolean.valueOf(prop.getProperty("autoexec.job.isallowed.manualtrigger.inadvance", "true"));
    }
}
