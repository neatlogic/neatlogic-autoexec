/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.initialdata.handler;

import codedriver.framework.initialdata.core.IInitialDataDefiner;

public class AutoexecInitialDataDefiner implements IInitialDataDefiner {
    @Override
    public String getModuleId() {
        return "autoexec";
    }

    @Override
    public String[] getTables() {
        return new String[]{
                "autoexec_risk",
                "autoexec_type"
        };
    }
}
