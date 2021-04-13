/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

/**
 * @author: linbq
 * @since: 2021/4/13 14:49
 **/
public class AutoexecCombopNameRepeatException extends ApiRuntimeException {

    private static final long serialVersionUID = -977867274722886183L;

    public AutoexecCombopNameRepeatException(String name){
        super("显示名：'" + name + "'已存在");
    }
}
