package codedriver.module.autoexec.api.catalog;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
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

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCatalogSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public String getToken() {
        return "autoexec/catalog/search";
    }

    @Override
    public String getName() {
        return "查询工具目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parentId", desc = "父id", type = ApiParamType.LONG),
            @Param(name = "keyword", desc = "关键词", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = AutoexecCatalogVo[].class, desc = "目录集合"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询工具目录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCatalogVo catalogVo = JSON.toJavaObject(jsonObj, AutoexecCatalogVo.class);
        int rowNum = autoexecCatalogMapper.searchAutoexecCatalogCount(catalogVo);
        if (rowNum > 0) {
            catalogVo.setRowNum(rowNum);
            return TableResultUtil.getResult(autoexecCatalogMapper.searchAutoexecCatalog(catalogVo), catalogVo);
        }
        return null;
    }
}
