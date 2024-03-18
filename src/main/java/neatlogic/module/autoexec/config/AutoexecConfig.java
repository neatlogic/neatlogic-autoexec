/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
