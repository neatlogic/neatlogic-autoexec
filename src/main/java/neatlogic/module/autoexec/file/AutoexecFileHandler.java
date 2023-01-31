/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.file;

import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

/**
 * 自动化模块附件处理器
 *
 * @author: linbq
 * @since: 2021/4/20 11:56
 **/
@Component
public class AutoexecFileHandler extends FileTypeHandlerBase {

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) throws PermissionDeniedException {
        if (!AuthActionChecker.check(AUTOEXEC_BASE.class)) {
            throw new PermissionDeniedException(AUTOEXEC_BASE.class);
        }
        return true;
    }

    @Override
    public String getName() {
        return "AUTOEXEC";
    }

    @Override
    public String getDisplayName() {
        return "自动化附件";
    }

    @Override
    public void afterUpload(FileVo fileVo, JSONObject jsonObj) {

    }

    @Override
    protected boolean myDeleteFile(FileVo fileVo, JSONObject paramObj) {
        return true;
    }
}
