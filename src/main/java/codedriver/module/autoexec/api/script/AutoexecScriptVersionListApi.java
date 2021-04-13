/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.AutoexecScriptVersionVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/version/list";
    }

    @Override
    public String getName() {
        return "获取脚本版本列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scriptId", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVersionVo[].class, desc = "版本列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "获取脚本版本列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVersionVo versionVo = JSON.toJavaObject(jsonObj, AutoexecScriptVersionVo.class);
        if (autoexecScriptMapper.checkScriptIsExistsById(versionVo.getScriptId()) == 0) {
            throw new AutoexecScriptNotFoundException(versionVo.getScriptId());
        }
        List<AutoexecScriptVersionVo> versionList = autoexecScriptMapper.getVersionList(versionVo);
        result.put("tbodyList", versionList);
        if (versionVo.getNeedPage()) {
            int rowNum = autoexecScriptMapper.getVersionCountByScriptId(versionVo.getScriptId());
            versionVo.setRowNum(rowNum);
            result.put("currentPage", versionVo.getCurrentPage());
            result.put("pageSize", versionVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, versionVo.getPageSize()));
            result.put("rowNum", versionVo.getRowNum());
        }

        return result;
    }


}
