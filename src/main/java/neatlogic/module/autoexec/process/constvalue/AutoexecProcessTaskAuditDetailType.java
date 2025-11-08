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
