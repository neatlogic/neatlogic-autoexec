/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class AutoexecScriptNameOrLabelRepeatException extends ApiRuntimeException {

    private static final long serialVersionUID = -2206138186638390045L;

    public AutoexecScriptNameOrLabelRepeatException(String name) {
        super("脚本：'" + name + "'已存在");
    }


}
