/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.operationauth.exception;

import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;

/**
 * @author linbq
 * @since 2022/3/1 11:27
 **/
public class ProcessTaskAutoexecHandlerNotEnableOperateException extends ProcessTaskPermissionDeniedException {
    private static final long serialVersionUID = 9216337410118158662L;

    public ProcessTaskAutoexecHandlerNotEnableOperateException(ProcessTaskOperationType operationType) {
        super("自动化节点不支持'" + operationType.getText() + "'操作");
    }
}
