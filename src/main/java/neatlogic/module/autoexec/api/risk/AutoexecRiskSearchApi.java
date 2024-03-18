/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.risk;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.OperateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
            Boolean hasAuth = AuthActionChecker.check(AUTOEXEC_MODIFY.class.getSimpleName());
            List<Long> idList = riskList.stream().map(AutoexecRiskVo::getId).collect(Collectors.toList());
            List<AutoexecRiskVo> referenceCountListForTool = autoexecRiskMapper.getReferenceCountListForTool(idList);
            List<AutoexecRiskVo> referenceCountListForScript = autoexecRiskMapper.getReferenceCountListForScript(idList);
            Map<Long, Integer> referenceCountForToolMap = new HashMap<>();
            Map<Long, Integer> referenceCountForScriptMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(referenceCountListForTool)) {
                referenceCountForToolMap = referenceCountListForTool.stream()
                        .collect(Collectors.toMap(AutoexecRiskVo::getId, AutoexecRiskVo::getReferenceCountForTool));
            }
            if (CollectionUtils.isNotEmpty(referenceCountListForScript)) {
                referenceCountForScriptMap = referenceCountListForScript.stream()
                        .collect(Collectors.toMap(AutoexecRiskVo::getId, AutoexecRiskVo::getReferenceCountForScript));
            }
            for (AutoexecRiskVo riskVo : riskList) {
                OperateVo edit = new OperateVo("edit", "编辑");
                OperateVo delete = new OperateVo("delete", "删除");
                riskVo.getOperateList().add(edit);
                riskVo.getOperateList().add(delete);
                Integer referenceCountForTool = referenceCountForToolMap.get(riskVo.getId());
                Integer referenceCountForScript = referenceCountForScriptMap.get(riskVo.getId());
                riskVo.setReferenceCountForTool(referenceCountForTool != null ? referenceCountForTool : 0);
                riskVo.setReferenceCountForScript(referenceCountForScript != null ? referenceCountForScript : 0);
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
