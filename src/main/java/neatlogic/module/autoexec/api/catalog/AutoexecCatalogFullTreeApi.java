package neatlogic.module.autoexec.api.catalog;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCatalogService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCatalogFullTreeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecCatalogService autoexecCatalogService;

    @Override
    public String getToken() {
        return "autoexec/catalog/fulltree";
    }

    @Override
    public String getName() {
        return "获取工具目录完整架构树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({

    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONOBJECT, explode = AutoexecCatalogVo.class),
    })
    @Description(desc = "获取工具目录完整架构树")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCatalogVo root = autoexecCatalogService.buildRootCatalog();
        List<AutoexecCatalogVo> list = autoexecCatalogMapper.getCatalogForTree(root.getLft(), root.getRht());
        if (!list.isEmpty()) {
            Map<Long, AutoexecCatalogVo> map = new HashMap<>();
            list.add(root);
            list.forEach(o -> map.put(o.getId(), o));
            for (AutoexecCatalogVo vo : list) {
                AutoexecCatalogVo parent = map.get(vo.getParentId());
                if (parent != null) {
                    vo.setParent(parent);
                }
            }
        }
        return root;
    }
}
