package neatlogic.module.autoexec.api.catalog;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCatalogTreeSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public String getToken() {
        return "autoexec/catalog/tree/search";
    }

    @Override
    public String getName() {
        return "查询工具目录(树结构)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键ID"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字"),
    })
    @Output({
            @Param(name = "children", type = ApiParamType.JSONARRAY, explode = AutoexecCatalogVo[].class, desc = "工具目录架构集合")
    })
    @Description(desc = "查询工具目录(树结构)")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        result.put("children", new ArrayList<>());
        Long id = jsonObj.getLong("id");
        String keyword = jsonObj.getString("keyword");
        List<AutoexecCatalogVo> catalogVoList = new ArrayList<>();
        List<Long> catalogIdList = new ArrayList<>();
        Map<Long, AutoexecCatalogVo> catalogVoMap = new HashMap<>();
        if (id != null) {
            AutoexecCatalogVo vo = autoexecCatalogMapper.getAutoexecCatalogById(id);
            if (vo == null) {
                throw new AutoexecCatalogNotFoundException(id);
            }
            catalogVoList = autoexecCatalogMapper.getParentListAndSelfByLR(vo.getLft(), vo.getRht());
            catalogVoList.forEach(o -> {
                catalogVoMap.put(o.getId(), o);
                catalogIdList.add(o.getId());
            });
        } else if (StringUtils.isNotBlank(keyword)) {
            AutoexecCatalogVo vo = new AutoexecCatalogVo();
            vo.setKeyword(keyword);
            List<AutoexecCatalogVo> targetCatalogList = autoexecCatalogMapper.searchAutoexecCatalog(vo);
            for (AutoexecCatalogVo catalogVo : targetCatalogList) {
                List<AutoexecCatalogVo> ancestorsAndSelf = autoexecCatalogMapper.getParentListAndSelfByLR(catalogVo.getLft(), catalogVo.getRht());
                for (AutoexecCatalogVo _catalogVo : ancestorsAndSelf) {
                    if (!catalogIdList.contains(_catalogVo.getId())) {
                        catalogVoMap.put(_catalogVo.getId(), _catalogVo);
                        catalogIdList.add(_catalogVo.getId());
                        catalogVoList.add(_catalogVo);
                    }
                }
            }
        } else {
            return result;
        }
        if (CollectionUtils.isNotEmpty(catalogVoList)) {
            AutoexecCatalogVo root = new AutoexecCatalogVo();
            root.setId(AutoexecCatalogVo.ROOT_ID);
            root.setName("root");
            root.setParentId(AutoexecCatalogVo.ROOT_PARENTID);
            catalogVoMap.put(root.getId(), root);
            List<AutoexecCatalogVo> childCountList = autoexecCatalogMapper.getAutoexecCatalogChildCountListByIdList(catalogIdList);
            Map<Long, Integer> childCountMap = new HashMap<>();
            childCountList.forEach(o -> childCountMap.put(o.getId(), o.getChildCount()));
            List<AutoexecCatalogVo> referenceCountForScriptList = autoexecCatalogMapper.getReferenceCountForScriptListByIdList(catalogIdList);
            Map<Long, Integer> referenceCountForScriptMap = new HashMap<>();
            referenceCountForScriptList.forEach(o -> referenceCountForScriptMap.put(o.getId(), o.getReferenceCountForScript()));
            catalogVoList.forEach(o -> {
                AutoexecCatalogVo parent = catalogVoMap.get(o.getParentId());
                if (parent != null) {
                    o.setParent(parent);
                }
                o.setChildCount(childCountMap.get(o.getId()));
                o.setReferenceCountForScript(referenceCountForScriptMap.get(o.getId()));
                // 计算目录本身以及子目录关联的脚本数，以此作为是否能删除的依据
                o.setReferenceCountOfSelfAndChildren(autoexecCatalogMapper.getReferenceCountForScriptOfSelfAndChildrenByLR(o.getLft(), o.getRht()));
            });
            result.put("children", root.getChildren());
        }
        return result;
    }
}
