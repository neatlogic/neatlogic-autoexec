/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class AutoexecScriptAuditVo extends BaseEditorVo {

    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "脚本ID", type = ApiParamType.LONG)
    private Long scriptId;
    @EntityField(name = "脚本版本ID", type = ApiParamType.LONG)
    private Long scriptVersionId;
    @EntityField(name = "操作类型", type = ApiParamType.STRING)
    private String operate;
    @EntityField(name = "内容hash", type = ApiParamType.STRING)
    private String contentHash;
    @EntityField(name = "内容", type = ApiParamType.STRING)
    private String content;

    public AutoexecScriptAuditVo() {
    }

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScriptId() {
        return scriptId;
    }

    public void setScriptId(Long scriptId) {
        this.scriptId = scriptId;
    }

    public Long getScriptVersionId() {
        return scriptVersionId;
    }

    public void setScriptVersionId(Long scriptVersionId) {
        this.scriptVersionId = scriptVersionId;
    }

    public String getOperate() {
        return operate;
    }

    public void setOperate(String operate) {
        this.operate = operate;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
