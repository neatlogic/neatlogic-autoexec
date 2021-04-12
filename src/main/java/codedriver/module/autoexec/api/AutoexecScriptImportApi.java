/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptImportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/import";
    }

    @Override
    public String getName() {
        return "导入脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id"),
    })
    @Description(desc = "导入脚本")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return null;
    }


}
