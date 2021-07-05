package codedriver.module.autoexec.fulltextindex;

import codedriver.framework.fulltextindex.core.IFullTextIndexType;

public enum FullTextIndexType implements IFullTextIndexType {
    SCRIPT_DOCUMENT_VERSION("script_document_version", "自定义工具版本");

    private final String type;
    private final String typeName;

    FullTextIndexType(String _type, String _typeName) {
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
        for (FullTextIndexType t : values()) {
            if (t.getType().equals(type)) {
                return t.getTypeName();
            }
        }
        return "";
    }

}
