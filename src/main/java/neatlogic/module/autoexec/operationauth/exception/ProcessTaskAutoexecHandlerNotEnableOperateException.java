/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.operationauth.exception;

import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;

/**
 * @author linbq
 * @since 2022/3/1 11:27
 **/
public class ProcessTaskAutoexecHandlerNotEnableOperateException extends ProcessTaskPermissionDeniedException {
    private static final long serialVersionUID = 9216337410118158662L;

    public ProcessTaskAutoexecHandlerNotEnableOperateException(String operationType) {
        super("自动化节点不支持“{0}”操作", operationType);
    }
}
