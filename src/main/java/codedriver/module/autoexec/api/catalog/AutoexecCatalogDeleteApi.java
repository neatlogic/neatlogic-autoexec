package codedriver.module.autoexec.api.catalog;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_CATALOG_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.exception.AutoexecCatalogHasBeenReferredException;
import codedriver.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.lrcode.LRCodeManager;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@AuthAction(action = AUTOEXEC_CATALOG_MODIFY.class)
@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecCatalogDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public String getToken() {
        return "autoexec/catalog/delete";
    }

    @Override
    public String getName() {
        return "删除工具目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "目录id", isRequired = true)
    })
    @Description(desc = "删除工具目录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCatalogVo vo = autoexecCatalogMapper.getAutoexecCatalogById(id);
        if (vo == null) {
            throw new AutoexecCatalogNotFoundException(id);
        }
        if (autoexecCatalogMapper.getReferenceCountByLR(vo.getLft(), vo.getRht()) > 0) {
            throw new AutoexecCatalogHasBeenReferredException(vo.getName());
        }
        LRCodeManager.beforeDeleteTreeNode("autoexec_catalog", "id", "parent_id", id);
        List<Long> idList = autoexecCatalogMapper.getChildrenIdListByLeftRightCode(vo.getLft(), vo.getRht());
        idList.add(id);
        autoexecCatalogMapper.deleteAutoexecCatalogByIdList(idList);
        return null;
    }

}
