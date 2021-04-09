/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;

public class AutoexecScriptVersionParamVo {

    @EntityField(name = "脚本版本ID", type = ApiParamType.LONG)
    private Long scriptVersionId;
    @EntityField(name = "参数名", type = ApiParamType.STRING)
    private String key;
    @EntityField(name = "参数默认值", type = ApiParamType.STRING)
    private String defaultValue;
    @EntityField(name = "参数类型(出参、入参)", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "表单类型", type = ApiParamType.STRING)
    private String handler;
    @EntityField(name = "是否必填(1:是;0:否)", type = ApiParamType.INTEGER)
    private Integer isRequired;
    @EntityField(name = "说明", type = ApiParamType.STRING)
    private String description;

    public AutoexecScriptVersionParamVo() {
    }

    public Long getScriptVersionId() {
        return scriptVersionId;
    }

    public void setScriptVersionId(Long scriptVersionId) {
        this.scriptVersionId = scriptVersionId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Integer getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Integer isRequired) {
        this.isRequired = isRequired;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
