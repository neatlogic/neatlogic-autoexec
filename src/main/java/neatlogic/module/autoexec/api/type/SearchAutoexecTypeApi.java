/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.type;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSON;
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
public class SearchAutoexecTypeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/search";
    }

    @Override
    public String getName() {
        return "查询自动化工具分类列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "isNeedCheckDataAuth", type = ApiParamType.INTEGER, desc = "是否校验数据权限（1：校验，0：不校验）")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecTypeVo[].class, desc = "分类列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询自动化工具分类列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecTypeVo typeVo = JSON.toJavaObject(jsonObj, AutoexecTypeVo.class);
        List<AutoexecTypeVo> typeList = autoexecTypeMapper.searchType(typeVo);
        result.put("tbodyList", typeList);
        if (CollectionUtils.isNotEmpty(typeList)) {
            List<Long> idList = typeList.stream().map(AutoexecTypeVo::getId).collect(Collectors.toList());
            List<AutoexecTypeVo> referenceCountListForTool = autoexecTypeMapper.getReferenceCountListForTool(idList);
            List<AutoexecTypeVo> referenceCountListForScript = autoexecTypeMapper.getReferenceCountListForScript(idList);
            List<AutoexecTypeVo> referenceCountListForCombop = autoexecTypeMapper.getReferenceCountListForCombop(idList);
            Map<Long, Integer> referenceCountForToolMap = new HashMap<>();
            Map<Long, Integer> referenceCountForScriptMap = new HashMap<>();
            Map<Long, Integer> referenceCountForCombopMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(referenceCountListForTool)) {
                referenceCountForToolMap = referenceCountListForTool.stream()
                        .collect(Collectors.toMap(AutoexecTypeVo::getId, AutoexecTypeVo::getReferenceCountForTool));
            }
            if (CollectionUtils.isNotEmpty(referenceCountListForScript)) {
                referenceCountForScriptMap = referenceCountListForScript.stream()
                        .collect(Collectors.toMap(AutoexecTypeVo::getId, AutoexecTypeVo::getReferenceCountForScript));
            }
            if (CollectionUtils.isNotEmpty(referenceCountListForCombop)) {
                referenceCountForCombopMap = referenceCountListForCombop.stream()
                        .collect(Collectors.toMap(AutoexecTypeVo::getId, AutoexecTypeVo::getReferenceCountForCombop));
            }
            for (AutoexecTypeVo vo : typeList) {
                Integer referenceCountForTool = referenceCountForToolMap.get(vo.getId());
                Integer referenceCountForScript = referenceCountForScriptMap.get(vo.getId());
                Integer referenceCountForCombop = referenceCountForCombopMap.get(vo.getId());
                vo.setReferenceCountForTool(referenceCountForTool != null ? referenceCountForTool : 0);
                vo.setReferenceCountForScript(referenceCountForScript != null ? referenceCountForScript : 0);
                vo.setReferenceCountForCombop(referenceCountForCombop != null ? referenceCountForCombop : 0);
            }
        }
        if (typeVo.getNeedPage()) {
            int rowNum = autoexecTypeMapper.searchTypeCount(typeVo);
            typeVo.setRowNum(rowNum);
            result.put("currentPage", typeVo.getCurrentPage());
            result.put("pageSize", typeVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, typeVo.getPageSize()));
            result.put("rowNum", typeVo.getRowNum());
        }
        return result;
    }


}
