/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class AutoexecScriptValidateTypeVo {

    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "高危代码ID", type = ApiParamType.LONG)
    private String validateId;
    @EntityField(name = "脚本类型", type = ApiParamType.STRING)
    private String scriptType;

    public AutoexecScriptValidateTypeVo() {
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

    public String getValidateId() {
        return validateId;
    }

    public void setValidateId(String validateId) {
        this.validateId = validateId;
    }

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }
}
