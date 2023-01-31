/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.fulltextindex;

import neatlogic.framework.fulltextindex.core.IFullTextIndexType;

public enum AutoexecFullTextIndexType implements IFullTextIndexType {
    SCRIPT_DOCUMENT_VERSION("script_document_version", "自定义工具版本");

    private final String type;
    private final String typeName;

    AutoexecFullTextIndexType(String _type, String _typeName) {
        type = _type;
        typeName = _typeName;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String getTypeName(String type) {
        for (AutoexecFullTextIndexType t : values()) {
            if (t.getType().equals(type)) {
                return t.getTypeName();
            }
        }
        return "";
    }

    @Override
    public boolean isActiveGlobalSearch() {
        return false;
    }

}
