/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 查询组合工具详情接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
//@Service
@Deprecated
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/get";
    }

    @Override
    public String getName() {
        return "查询组合工具详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(explode = AutoexecCombopVo.class, desc = "组合工具详情")
    })
    @Description(desc = "查询组合工具详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopService.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        // owner字段必须在校验权限后，再加上前缀user#
        autoexecCombopVo.setOwner(GroupSearch.USER.getValuePlugin() + autoexecCombopVo.getOwner());
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                autoexecCombopService.needExecuteConfig(autoexecCombopVo, combopPhaseVo);
            }
        }
        return autoexecCombopVo;
    }
}
