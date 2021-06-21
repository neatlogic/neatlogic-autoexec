/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptAndToolOperate;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dto.OperateVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
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
        int rowNum = autoexecScriptMapper.searchHistoricalVersionCountByScriptIdAndStatus(vo);
        List<AutoexecScriptVersionVo> list = autoexecScriptMapper.searchHistoricalVersionListByScriptIdAndStatus(vo);
        // 没有编辑权限，则不显示未审批通过版本列表
        if (Objects.equals(vo.getStatus(), "notPassed") && !AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            rowNum = 0;
            list.clear();
        }
        if (Objects.equals(vo.getStatus(), "passed") && CollectionUtils.isNotEmpty(list)) {
            Boolean hasManageAuth = AuthActionChecker.check(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName());
            Iterator<AutoexecScriptVersionVo> iterator = list.iterator();
            while (iterator.hasNext()) {
                AutoexecScriptVersionVo next = iterator.next();
                // 在历史版本列表中移除当前激活版本
                if (Objects.equals(next.getIsActive(), 1)) {
                    iterator.remove();
                    continue;
                }
                List<OperateVo> operateList = new ArrayList<>();
                next.setOperateList(operateList);
                OperateVo switchVersion = new OperateVo(ScriptAndToolOperate.SWITCH_VERSION.getValue(), ScriptAndToolOperate.SWITCH_VERSION.getText());
                OperateVo compare = new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText());
                OperateVo delete = new OperateVo(ScriptAndToolOperate.VERSION_DELETE.getValue(), ScriptAndToolOperate.VERSION_DELETE.getText());
                operateList.add(switchVersion);
                operateList.add(compare);
                operateList.add(delete);
                if (!hasManageAuth) {
                    switchVersion.setDisabled(1);
                    switchVersion.setDisabledReason("无权限，请联系管理员");
                    delete.setDisabled(1);
                    delete.setDisabledReason("无权限，请联系管理员");
                }
            }
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
