/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.process.constvalue;

import neatlogic.framework.process.audithandler.core.IProcessTaskAuditDetailType;

public enum AutoexecProcessTaskAuditDetailType implements IProcessTaskAuditDetailType {
    AUTOEXECMESSAGE("autoexecmessage", "", "autoexecMessage", "oldAutoexecMessage", 22, false),
    ;

    private String value;
    private String text;
    private String paramName;
    private String oldDataParamName;
    private int sort;
    private boolean needCompression;

    AutoexecProcessTaskAuditDetailType(String value, String text, String paramName, String oldDataParamName, int sort, boolean needCompression) {
        this.value = value;
        this.text = text;
        this.paramName = paramName;
        this.oldDataParamName = oldDataParamName;
        this.sort = sort;
        this.needCompression = needCompression;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getParamName() {
        return paramName;
    }

    @Override
    public String getOldDataParamName() {
        return oldDataParamName;
    }

    @Override
    public int getSort() {
        return sort;
    }

    @Override
    public boolean getNeedCompression() {
        return needCompression;
    }
}
