package codedriver.module.autoexec.api.catalog;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCatalogTreeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public String getToken() {
        return "autoexec/catalog/tree";
    }

    @Override
    public String getName() {
        return "获取工具目录架构树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parentId", desc = "父id", type = ApiParamType.LONG),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "needPage", desc = "是否分页", type = ApiParamType.BOOLEAN),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = AutoexecCatalogVo[].class, desc = "目录集合"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "获取工具目录架构树")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCatalogVo catalogVo = JSON.toJavaObject(jsonObj, AutoexecCatalogVo.class);
        Long parentId = jsonObj.getLong("parentId");
        if (catalogVo.getParentId() != null) {
            if (autoexecCatalogMapper.checkAutoexecCatalogIsExists(parentId) == 0) {
                throw new AutoexecCatalogNotFoundException(parentId);
            }
        } else {
            parentId = AutoexecCatalogVo.ROOT_ID;
        }
        catalogVo.setParentId(parentId);
        int rowNum = autoexecCatalogMapper.searchAutoexecCatalogCount(catalogVo);
        if (rowNum > 0) {
            catalogVo.setRowNum(rowNum);
            List<AutoexecCatalogVo> list = autoexecCatalogMapper.searchAutoexecCatalog(catalogVo);
            List<Long> idList = list.stream().map(AutoexecCatalogVo::getId).collect(Collectors.toList());
            List<AutoexecCatalogVo> childCountList = autoexecCatalogMapper.getAutoexecCatalogChildCountListByIdList(idList);
            Map<Long, Integer> childCountMap = new HashMap<>();
            childCountList.forEach(o -> childCountMap.put(o.getId(), o.getChildCount()));
            List<AutoexecCatalogVo> referenceCountForScriptList = autoexecCatalogMapper.getReferenceCountForScriptListByIdList(idList);
            Map<Long, Integer> referenceCountForScriptMap = new HashMap<>();
            referenceCountForScriptList.forEach(o -> referenceCountForScriptMap.put(o.getId(), o.getReferenceCountForScript()));
            list.forEach(o -> {
                o.setChildCount(childCountMap.get(o.getId()));
                o.setReferenceCountForScript(referenceCountForScriptMap.get(o.getId()));
                // 计算目录本身以及子目录关联的脚本数，以此作为是否能删除的依据
                o.setReferenceCountOfSelfAndChildren(autoexecCatalogMapper.getReferenceCountForScriptOfSelfAndChildrenByLR(o.getLft(), o.getRht()));
            });
            return TableResultUtil.getResult(list, catalogVo);
        }
        return null;
    }
}
