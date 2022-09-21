package codedriver.module.autoexec.api.catalog;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecCatalogRepeatException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.lrcode.LRCodeManager;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

    @Override
    public String getToken() {
        return "autoexec/catalog/save";
    }

    @Override
    public String getName() {
        return "保存工具目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "目录id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, desc = "名称", maxLength = 50, isRequired = true, xss = true),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "父目录id"),
    })
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "目录id")})
    @Description(desc = "保存工具目录")
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
            autoexecCatalogMapper.updateAutoexecCatalogNameById(vo);
        } else {
            if (!AutoexecCatalogVo.ROOT_ID.equals(vo.getParentId()) && autoexecCatalogMapper.checkAutoexecCatalogIsExists(vo.getParentId()) == 0) {
                throw new AutoexecCatalogNotFoundException(vo.getParentId());
            }
            int lft = LRCodeManager.beforeAddTreeNode("autoexec_catalog", "id", "parent_id", vo.getParentId());
            vo.setParentId(vo.getParentId());
            vo.setLft(lft);
            vo.setRht(lft + 1);
            autoexecCatalogMapper.insertAutoexecCatalog(vo);
        }
        return null;
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
