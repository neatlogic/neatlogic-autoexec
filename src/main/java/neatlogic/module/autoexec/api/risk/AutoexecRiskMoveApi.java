/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.risk;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import neatlogic.framework.autoexec.exception.AutoexecRiskNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecRiskMoveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public String getToken() {
        return "autoexec/risk/move";
    }

    @Override
    public String getName() {
        return "移动操作级别";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id"),
            @Param(name = "sort", type = ApiParamType.INTEGER, isRequired = true, desc = "移动后的序号")
    })
    @Output({
    })
    @Description(desc = "移动操作级别")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecRiskVo vo = autoexecRiskMapper.getAutoexecRiskById(id);
        if (vo == null) {
            throw new AutoexecRiskNotFoundException(id);
        }
        Integer oldSort = vo.getSort();
        Integer newSort = jsonObj.getInteger("sort");
        if (oldSort < newSort) {//往后移动
            autoexecRiskMapper.updateSortDecrement(oldSort, newSort);
        } else if (oldSort > newSort) {//往前移动
            autoexecRiskMapper.updateSortIncrement(newSort, oldSort);
        }
        vo.setSort(newSort);
        autoexecRiskMapper.updateRisk(vo);
        return null;
    }

}
