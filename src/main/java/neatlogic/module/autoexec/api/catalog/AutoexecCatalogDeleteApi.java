package neatlogic.module.autoexec.api.catalog;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogHasBeenReferredException;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@AuthAction(action = AUTOEXEC_MODIFY.class)
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
        List<Long> idList = autoexecCatalogMapper.getChildrenIdListByLeftRightCode(vo.getLft(), vo.getRht());
        idList.add(id);
        LRCodeManager.beforeDeleteTreeNode("autoexec_catalog", "id", "parent_id", id);
        autoexecCatalogMapper.deleteAutoexecCatalogByIdList(idList);
        return null;
    }

}
