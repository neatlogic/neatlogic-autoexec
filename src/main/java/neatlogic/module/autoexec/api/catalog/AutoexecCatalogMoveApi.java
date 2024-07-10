package neatlogic.module.autoexec.api.catalog;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCatalogRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.lrcode.constvalue.MoveType;
import neatlogic.framework.lrcode.dao.mapper.TreeMapper;
import neatlogic.framework.lrcode.dto.TreeNodeVo;
import neatlogic.framework.lrcode.exception.MoveTargetNodeIllegalException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@AuthAction(action = AUTOEXEC_MODIFY.class)
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCatalogMoveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private TreeMapper treeMapper;

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
        Long id = jsonObj.getLong("id");
        AutoexecCatalogVo autoexecCatalog = autoexecCatalogMapper.getAutoexecCatalogById(id);
        if (autoexecCatalog == null) {
            throw new AutoexecCatalogNotFoundException(id);
        }
        Long parentId = jsonObj.getLong("parentId");
        if (Objects.equal(id, parentId)) {
            throw new MoveTargetNodeIllegalException();
        }
        if (parentId == null) {
            parentId = AutoexecCatalogVo.ROOT_ID;
        } else if (!AutoexecCatalogVo.ROOT_ID.equals(parentId)) {
            AutoexecCatalogVo parent = autoexecCatalogMapper.getAutoexecCatalogById(parentId);
            if (parent == null) {
                throw new AutoexecCatalogNotFoundException(parentId);
            }
        }
        AutoexecCatalogVo vo = new AutoexecCatalogVo(autoexecCatalog.getId(), autoexecCatalog.getName(), parentId);
        if (autoexecCatalogMapper.checkAutoexecCatalogNameIsRepeat(vo) > 0) {
            throw new AutoexecCatalogRepeatException(autoexecCatalog.getName());
        }
        Long targetId;
        MoveType moveType;
        int sort = jsonObj.getIntValue("sort");
        AutoexecCatalogVo next = autoexecCatalogMapper.getAutoexecCatalogByParentIdAndStartNum(parentId, sort);
        if (next == null) {
            targetId = parentId;
            moveType = MoveType.INNER;
        } else {
            if (next.getId().equals(id)) {
                return null;
            } else {
                targetId = next.getId();
                if (next.getParentId().equals(autoexecCatalog.getParentId())) {
                    if (next.getLft() < autoexecCatalog.getLft()) {
                        moveType = MoveType.PREV;
                    } else {
                        moveType = MoveType.NEXT;
                    }
                } else {
                    moveType = MoveType.PREV;
                }
            }
        }
        int[] lftRht = LRCodeManager.moveTreeNode("autoexec_catalog", "id", "parent_id", id, moveType, targetId);
        if (!targetId.equals(autoexecCatalog.getParentId()) && lftRht != null) {
            TreeNodeVo treeNodeVo = new TreeNodeVo("autoexec_catalog", lftRht[0], lftRht[1]);
            treeMapper.updateUpwardIdPathByLftRht(treeNodeVo);
            treeMapper.updateUpwardNamePathByLftRht(treeNodeVo);
        }
        return null;
    }

}
