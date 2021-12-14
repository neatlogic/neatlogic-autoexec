package codedriver.module.autoexec.api.catalog;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_CATALOG_MODIFY;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AuthAction(action = AUTOEXEC_CATALOG_MODIFY.class)
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCatalogMoveApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "autoexec/catalog/move";
    }

    @Override
    public String getName() {
        return "移动工具目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "目录id", isRequired = true),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "父id", isRequired = true, minLength = 1),
            @Param(name = "sort", type = ApiParamType.INTEGER, desc = "sort(目标父级的位置，从0开始)", isRequired = true)
    })
    @Description(desc = "移动工具目录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        return null;
    }

}
