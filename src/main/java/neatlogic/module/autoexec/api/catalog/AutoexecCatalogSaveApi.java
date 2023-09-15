package neatlogic.module.autoexec.api.catalog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCatalogRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.service.AutoexecCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@AuthAction(action = AUTOEXEC_MODIFY.class)
@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecCatalogSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;
    @Resource
    private AutoexecCatalogService autoexecCatalogService;

    @Override
    public String getToken() {
        return "autoexec/catalog/save";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccatalogsaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, desc = "common.name", maxLength = 50, isRequired = true, xss = true),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "common.parentid"),
    })
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "common.id")})
    @Description(desc = "nmaac.autoexeccatalogsaveapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCatalogVo vo = JSON.toJavaObject(jsonObj, AutoexecCatalogVo.class);
        if (vo.getParentId() == null) {
            vo.setParentId(AutoexecCatalogVo.ROOT_ID);
        }
        // 同级下不重复
        if (autoexecCatalogMapper.checkAutoexecCatalogNameIsRepeat(vo) > 0) {
            throw new AutoexecCatalogRepeatException(vo.getName());
        }
        if (id != null) {
            if (autoexecCatalogMapper.checkAutoexecCatalogIsExists(id) == 0) {
                throw new AutoexecCatalogNotFoundException(id);
            }
        }
        return autoexecCatalogService.saveAutoexecCatalog(vo);
//        if (id != null) {
//            if (autoexecCatalogMapper.checkAutoexecCatalogIsExists(id) == 0) {
//                throw new AutoexecCatalogNotFoundException(id);
//            }
//            autoexecCatalogMapper.updateAutoexecCatalogNameById(vo);
//        } else {
//            if (!AutoexecCatalogVo.ROOT_ID.equals(vo.getParentId()) && autoexecCatalogMapper.checkAutoexecCatalogIsExists(vo.getParentId()) == 0) {
//                throw new AutoexecCatalogNotFoundException(vo.getParentId());
//            }
//            int lft = LRCodeManager.beforeAddTreeNode("autoexec_catalog", "id", "parent_id", vo.getParentId());
//            vo.setParentId(vo.getParentId());
//            vo.setLft(lft);
//            vo.setRht(lft + 1);
//            autoexecCatalogMapper.insertAutoexecCatalog(vo);
//        }
//        return vo.getId();
    }

    public IValid name() {
        return value -> {
            AutoexecCatalogVo vo = JSON.toJavaObject(value, AutoexecCatalogVo.class);
            if (vo.getParentId() == null) {
                vo.setParentId(AutoexecCatalogVo.ROOT_ID);
            }
            if (autoexecCatalogMapper.checkAutoexecCatalogNameIsRepeat(vo) > 0) {
                return new FieldValidResultVo(new AutoexecCatalogRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
