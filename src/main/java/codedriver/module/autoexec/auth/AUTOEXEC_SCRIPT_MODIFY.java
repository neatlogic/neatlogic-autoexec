/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.auth;

import codedriver.framework.auth.core.AuthBase;

public class AUTOEXEC_SCRIPT_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "脚本维护权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "对脚本进行查看、编辑和复制";
    }

    @Override
    public String getAuthGroup() {
        return "autoexec";
    }
}
