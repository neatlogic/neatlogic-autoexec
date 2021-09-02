/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.constvalue;

import codedriver.framework.process.stephandler.core.IProcessStepHandlerType;

/**
 * @author linbq
 * @since 2021/9/2 14:40
 **/
public enum AutoexecProcessStepHandlerType implements IProcessStepHandlerType {
    AUTOEXEC("autoexec", "process", "自动化");
    private String handler;
    private String name;
    private String type;

    AutoexecProcessStepHandlerType(String handler, String type, String name) {
        this.handler = handler;
        this.name = name;
        this.type = type;
    }
    @Override
    public String getHandler() {
        return handler;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }
}
