/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

public class AutoexecScriptLineContentVo {

    @EntityField(name = "内容hash值", type = ApiParamType.STRING)
    private String hash;
    @EntityField(name = "行脚本内容", type = ApiParamType.STRING)
    private String content;

    public AutoexecScriptLineContentVo() {
    }

    public AutoexecScriptLineContentVo(String content) {
        this.content = content;
    }

    public String getHash() {
        if (StringUtils.isBlank(hash) && StringUtils.isNotBlank(content)) {
            hash = DigestUtils.md5DigestAsHex(content.getBytes());
        }
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
