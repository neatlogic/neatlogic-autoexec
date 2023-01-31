/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 查询组合工具顶层参数列表接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
//@Service
@Deprecated
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/combop/param/list";
    }

    @Override
    public String getName() {
        return "查询组合工具顶层参数列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(explode = AutoexecCombopParamVo[].class, desc = "参数列表")
    })
    @Description(desc = "查询组合工具顶层参数列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        List<AutoexecParamVo> autoexecParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
        for (AutoexecParamVo autoexecParamVo : autoexecParamVoList) {
            autoexecService.mergeConfig(autoexecParamVo);
        }
        return autoexecParamVoList;
    }
}
