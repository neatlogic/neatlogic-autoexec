/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.dto.OperateVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.operate.ScriptOperateManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/search";
    }

    @Override
    public String getName() {
        return "查询脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "versionStatus", type = ApiParamType.ENUM, rule = "draft,submitted,passed,rejected", desc = "状态"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVo[].class, desc = "脚本列表"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "已通过、草稿、待审批、已驳回状态的数量"),
            @Param(name = "operateList", type = ApiParamType.JSONARRAY, desc = "操作按钮"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        List<AutoexecScriptVo> scriptVoList = autoexecScriptMapper.searchScript(scriptVo);
        result.put("tbodyList", scriptVoList);
        // 获取操作权限
        if (Objects.equals(ScriptVersionStatus.PASSED.getValue(), scriptVo.getVersionStatus()) && CollectionUtils.isNotEmpty(scriptVoList)) {
            List<Long> idList = scriptVoList.stream().map(AutoexecScriptVo::getId).collect(Collectors.toList());
            ScriptOperateManager.Builder builder = new ScriptOperateManager().new Builder();
            builder.addScriptId(idList.toArray(new Long[idList.size()]));
            Map<Long, List<OperateVo>> operateListMap = builder.managerBuild().getOperateListMap();
            if (MapUtils.isNotEmpty(operateListMap)) {
                scriptVoList.stream().forEach(o -> o.setOperateList(operateListMap.get(o.getId())));
            }
        }
        if (scriptVo.getNeedPage()) {
            int rowNum = autoexecScriptMapper.searchScriptCount(scriptVo);
            scriptVo.setRowNum(rowNum);
            result.put("currentPage", scriptVo.getCurrentPage());
            result.put("pageSize", scriptVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, scriptVo.getPageSize()));
            result.put("rowNum", scriptVo.getRowNum());
        }
        // 分别查询含有已通过、草稿、待审批、已驳回状态的脚本数量
        JSONArray statusList = new JSONArray();
        scriptVo.setVersionStatus(ScriptVersionStatus.PASSED.getValue());
        statusList.add(new JSONObject() {{
            this.put("text", ScriptVersionStatus.PASSED.getText());
            this.put("value", ScriptVersionStatus.PASSED.getValue());
            this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
        }});
        scriptVo.setVersionStatus(ScriptVersionStatus.DRAFT.getValue());
        statusList.add(new JSONObject() {{
            this.put("text", ScriptVersionStatus.DRAFT.getText());
            this.put("value", ScriptVersionStatus.DRAFT.getValue());
            this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
        }});
        scriptVo.setVersionStatus(ScriptVersionStatus.SUBMITTED.getValue());
        statusList.add(new JSONObject() {{
            this.put("text", ScriptVersionStatus.SUBMITTED.getText());
            this.put("value", ScriptVersionStatus.SUBMITTED.getValue());
            this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
        }});
        scriptVo.setVersionStatus(ScriptVersionStatus.REJECTED.getValue());
        statusList.add(new JSONObject() {{
            this.put("text", ScriptVersionStatus.REJECTED.getText());
            this.put("value", ScriptVersionStatus.REJECTED.getValue());
            this.put("count", autoexecScriptMapper.searchScriptCount(scriptVo));
        }});
        result.put("statusList", statusList);
        return result;
    }


}
