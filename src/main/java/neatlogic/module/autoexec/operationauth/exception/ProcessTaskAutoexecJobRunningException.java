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

package neatlogic.module.autoexec.operationauth.exception;

import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;

/**
 * @author linbq
 * @since 2022/3/1 11:27
 **/
public class ProcessTaskAutoexecJobRunningException extends ProcessTaskPermissionDeniedException {
    private static final long serialVersionUID = 9216337410118158764L;

    public ProcessTaskAutoexecJobRunningException() {
        super("自动化作业正在执行，无法人工流转");
    }
}
