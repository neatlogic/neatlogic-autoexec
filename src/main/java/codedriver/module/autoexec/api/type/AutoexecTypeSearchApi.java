/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.type;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecTypeSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/search";
    }

    @Override
    public String getName() {
        return "查询插件类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecTypeVo[].class, desc = "类型列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询插件类型")
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
            if (CollectionUtils.isNotEmpty(referenceCountListForTool)) {
                for (AutoexecTypeVo vo : referenceCountListForTool) {
                    typeList.stream().forEach(o -> {
                        if (Objects.equals(o.getId(), vo.getId())) {
                            o.setReferenceCountForTool(vo.getReferenceCountForTool());
                        }
                    });
                }
            }
            if (CollectionUtils.isNotEmpty(referenceCountListForScript)) {
                for (AutoexecTypeVo vo : referenceCountListForScript) {
                    typeList.stream().forEach(o -> {
                        if (Objects.equals(o.getId(), vo.getId())) {
                            o.setReferenceCountForScript(vo.getReferenceCountForScript());
                        }
                    });
                }
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
