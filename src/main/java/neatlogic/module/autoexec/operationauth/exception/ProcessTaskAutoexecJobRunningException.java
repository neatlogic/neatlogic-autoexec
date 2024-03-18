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
