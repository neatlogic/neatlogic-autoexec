/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
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
            @Param(name = "status", type = ApiParamType.ENUM, rule = "notPassed,passed", isRequired = true, desc = "是否已经审核通过"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVersionVo[].class, desc = "根据status返回未通过版本列表或历史版本列表"),
    })
    @Description(desc = "获取脚本版本列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVersionVo vo = jsonObj.toJavaObject(AutoexecScriptVersionVo.class);
        if (autoexecScriptMapper.checkScriptIsExistsById(vo.getScriptId()) == 0) {
            throw new AutoexecScriptNotFoundException(vo.getScriptId());
        }
        int rowNum = 0;
        List<AutoexecScriptVersionVo> list = new ArrayList<>();
        // 没有编辑权限，则不显示未审批通过版本列表
        if (Objects.equals(vo.getStatus(), "notPassed") && AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            rowNum = autoexecScriptMapper.searchHistoricalVersionCountByScriptIdAndStatus(vo);
            list = autoexecScriptMapper.searchHistoricalVersionListByScriptIdAndStatus(vo);
        } else if (Objects.equals(vo.getStatus(), "passed")) {
            rowNum = autoexecScriptMapper.searchHistoricalVersionCountByScriptIdAndStatus(vo);
            list = autoexecScriptMapper.searchHistoricalVersionListByScriptIdAndStatus(vo);
        }
        vo.setRowNum(rowNum);
        result.put("currentPage", vo.getCurrentPage());
        result.put("pageSize", vo.getPageSize());
        result.put("pageCount", PageUtil.getPageCount(rowNum, vo.getPageSize()));
        result.put("rowNum", vo.getRowNum());
        result.put("tbodyList", list);

        return result;
    }
}
