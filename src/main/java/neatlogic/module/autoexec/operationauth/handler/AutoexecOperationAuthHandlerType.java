/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.operationauth.handler;

import neatlogic.framework.process.operationauth.core.IOperationAuthHandlerType;
import neatlogic.framework.util.$;

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
        return $.t(text);
    }
}
