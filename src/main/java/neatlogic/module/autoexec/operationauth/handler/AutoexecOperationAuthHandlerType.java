/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.operationauth.handler;

import neatlogic.framework.process.operationauth.core.IOperationAuthHandlerType;

/**
 * @author linbq
 * @since 2021/9/8 17:50
 **/
public enum AutoexecOperationAuthHandlerType implements IOperationAuthHandlerType {
    AUTOEXEC("autoexec", "自动化");

    AutoexecOperationAuthHandlerType(String value, String text){
        this.value = value;
        this.text = text;
    }

    private String value;
    private String text;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return text;
    }
}
