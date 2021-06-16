/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.risk;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_RISK_MODIFY;
import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dto.OperateVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecRiskSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public String getToken() {
        return "autoexec/risk/search";
    }

    @Override
    public String getName() {
        return "查询操作级别";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "状态"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(type = ApiParamType.JSONARRAY, explode = AutoexecRiskVo[].class, desc = "操作级别列表"),
            @Param(explode = BasePageVo[].class)
    })
    @Description(desc = "查询操作级别")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        AutoexecRiskVo vo = jsonObj.toJavaObject(AutoexecRiskVo.class);
        int rowNum = autoexecRiskMapper.searchRiskCount(vo);
        List<AutoexecRiskVo> riskList = autoexecRiskMapper.searchRisk(vo);
        resultObj.put("tbodyList", riskList);
        if (CollectionUtils.isNotEmpty(riskList)) {
            Boolean hasAuth = AuthActionChecker.check(AUTOEXEC_RISK_MODIFY.class.getSimpleName());
            List<Long> idList = riskList.stream().map(AutoexecRiskVo::getId).collect(Collectors.toList());
            List<AutoexecRiskVo> referenceCountListForTool = autoexecRiskMapper.getReferenceCountListForTool(idList);
            List<AutoexecRiskVo> referenceCountListForScript = autoexecRiskMapper.getReferenceCountListForScript(idList);
            Map<Long, Integer> referenceCountForToolMap = new HashMap<>();
            Map<Long, Integer> referenceCountForScriptMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(referenceCountListForTool)) {
                if (CollectionUtils.isNotEmpty(referenceCountListForTool)) {
                    referenceCountForToolMap = referenceCountListForTool.stream()
                            .collect(Collectors.toMap(AutoexecRiskVo::getId, AutoexecRiskVo::getReferenceCountForTool));
                }
                if (CollectionUtils.isNotEmpty(referenceCountListForScript)) {
                    referenceCountForScriptMap = referenceCountListForScript.stream()
                            .collect(Collectors.toMap(AutoexecRiskVo::getId, AutoexecRiskVo::getReferenceCountForScript));
                }
            }
            for (AutoexecRiskVo riskVo : riskList) {
                OperateVo edit = new OperateVo("edit", "编辑");
                OperateVo delete = new OperateVo("delete", "删除");
                riskVo.getOperateList().add(edit);
                riskVo.getOperateList().add(delete);
                Integer referenceCountForTool = referenceCountForToolMap.get(riskVo.getId());
                Integer referenceCountForScript = referenceCountForScriptMap.get(riskVo.getId());
                if (hasAuth) {
                    if ((referenceCountForTool != null && referenceCountForTool > 0) || (referenceCountForScript != null && referenceCountForScript > 0)) {
                        delete.setDisabled(1);
                        delete.setDisabledReason("当前操作级别已被引用，不可删除");
                    }
                } else {
                    edit.setDisabled(1);
                    edit.setDisabledReason("无权限，请联系管理员");
                    delete.setDisabled(1);
                    delete.setDisabledReason("无权限，请联系管理员");
                }
            }
        }
        int pageCount = PageUtil.getPageCount(rowNum, vo.getPageSize());
        vo.setPageCount(pageCount);
        vo.setRowNum(rowNum);
        resultObj.put("currentPage", vo.getCurrentPage());
        resultObj.put("pageSize", vo.getPageSize());
        resultObj.put("pageCount", pageCount);
        resultObj.put("rowNum", rowNum);
        return resultObj;
    }


}
