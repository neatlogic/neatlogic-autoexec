/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class AutoexecTypeHasBeenReferredException extends ApiRuntimeException {

    private static final long serialVersionUID = -977868274722886183L;

    public AutoexecTypeHasBeenReferredException(String name) {
        super("插件类型：'" + name + "'已经被工具或脚本引用");
    }


}
